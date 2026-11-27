package apptive.fin.notification.scheduler;

import apptive.fin.notification.dto.*;
import apptive.fin.notification.entity.*;
import apptive.fin.notification.repository.EmailSendLogRepository;
import apptive.fin.notification.repository.NotificationScheduleRepository;
import apptive.fin.notification.service.EmailTemplateService;
import apptive.fin.notification.service.UnsubscribeService;
import apptive.fin.search.entity.Product;
import apptive.fin.search.repository.ProductRepository;
import apptive.fin.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랜잭션이 필요한 스케줄 처리 로직을 분리한 헬퍼 클래스.
 * Spring AOP 프록시가 정상 적용되도록 별도 빈으로 분리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduleTxHelper {

    private final NotificationScheduleRepository scheduleRepository;
    private final EmailSendLogRepository sendLogRepository;
    private final EmailTemplateService templateService;
    private final UnsubscribeService unsubscribeService;
    private final ProductRepository productRepository;

    /**
     * [TX 1] 작업 선점 - SCHEDULED → SENDING 상태 변경
     */
    @Transactional
    public EmailSendContext claimSchedule(Long scheduleId) {
        int updated = scheduleRepository.updateStatusConditionally(
                scheduleId, ScheduleStatus.SCHEDULED, ScheduleStatus.SENDING);

        if (updated == 0) {
            log.debug("Schedule {} already claimed or processed", scheduleId);
            return null;
        }

        NotificationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        NotificationSetting setting = schedule.getSetting();
        User user = setting.getUser();
        String email = user.getEmail();

        SkipReason skipReason = checkSkipConditions(user, email);
        if (skipReason != null) {
            return EmailSendContext.skipped(scheduleId, skipReason);
        }

        String unsubscribeUrl = unsubscribeService.generateUnsubscribeUrl(email);
        Product product = productRepository.findById(setting.getTargetId()).orElse(null);
        String productName = product != null ? product.getDisplayProductName() : "상품";
        int dday = parseDday(schedule.getOffsetLabel());
        String subject = buildSubject(setting.getNotificationType(), productName, dday);
        String htmlBody = buildHtmlBody(setting, product, dday, unsubscribeUrl);
        MailType mailType = toMailType(setting.getNotificationType());

        return EmailSendContext.ready(scheduleId, email, subject, htmlBody, mailType);
    }

    /**
     * [TX 2] 결과에 따라 SENT/FAILED/SKIPPED 상태 변경 및 로그 저장
     */
    @Transactional
    public void completeSchedule(Long scheduleId, EmailSendContext context, EmailSendResult result) {
        NotificationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        if (context.skipReason() != null) {
            schedule.markAsSkipped();
            log.info("Schedule {} skipped: {}", scheduleId, context.skipReason());
            return;
        }

        sendLogRepository.save(EmailSendLog.builder()
                .schedule(schedule)
                .toEmail(context.email())
                .mailType(context.mailType())
                .resendMessageId(result.messageId())
                .status(result.success() ? EmailSendStatus.SENT : EmailSendStatus.FAILED)
                .errorMessage(result.errorMessage())
                .build());

        if (result.success()) {
            schedule.markAsSent();
            log.info("Notification sent to {} for schedule {}", context.email(), scheduleId);
        } else {
            schedule.markAsFailed();
            log.error("Failed to send notification for schedule {}: {}", scheduleId, result.errorMessage());
        }
    }

    private SkipReason checkSkipConditions(User user, String email) {
        if (email == null || email.isBlank()) {
            log.warn("User {} has no email, skipping notification", user.getId());
            return SkipReason.NO_EMAIL;
        }
        if (!user.isEmailVerified()) {
            log.warn("User {} email not verified, skipping notification", user.getId());
            return SkipReason.NOT_VERIFIED;
        }
        if (unsubscribeService.isUnsubscribed(email)) {
            log.info("Email {} is unsubscribed, skipping notification", email);
            return SkipReason.UNSUBSCRIBED;
        }
        return null;
    }

    private String buildSubject(NotificationType type, String productName, int dday) {
        return switch (type) {
            case DEADLINE -> templateService.buildDeadlineSubject(productName, dday);
            case START -> templateService.buildStartSubject(productName, dday == 0);
            case MATURITY -> templateService.buildMaturitySubject(productName, dday);
        };
    }

    private int parseDday(String offsetLabel) {
        if (offsetLabel == null || offsetLabel.isBlank()) {
            return 0;
        }
        String numericPart = offsetLabel.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String buildHtmlBody(NotificationSetting setting, Product product, int dday, String unsubscribeUrl) {
        String productName = product != null ? product.getDisplayProductName() : "상품";
        String detailUrl = "https://y-fin.com/products/" + setting.getTargetId();
        String allSchedulesUrl = "https://y-fin.com/schedules";
        String manageUrl = "https://y-fin.com/settings/notifications";
        String bankName = product != null && product.getSource() != null
                ? product.getSource().getName() : "";

        return switch (setting.getNotificationType()) {
            case DEADLINE -> templateService.buildDeadlineEmailV2(DeadlineEmailData.builder()
                    .dday(dday)
                    .urgent(dday <= 3)
                    .title(productName)
                    .agency(bankName)
                    .ctaUrl(detailUrl)
                    .allSchedulesUrl(allSchedulesUrl)
                    .manageUrl(manageUrl)
                    .unsubscribeUrl(unsubscribeUrl)
                    .build());
            case START -> templateService.buildStartEmailV2(StartEmailData.builder()
                    .isToday(dday == 0)
                    .title(productName)
                    .agency(bankName)
                    .ctaUrl(detailUrl)
                    .allSchedulesUrl(allSchedulesUrl)
                    .manageUrl(manageUrl)
                    .unsubscribeUrl(unsubscribeUrl)
                    .build());
            case MATURITY -> templateService.buildMaturityEmailV2(MaturityEmailData.builder()
                    .dday(dday)
                    .productName(productName)
                    .compareUrl("https://y-fin.com/compare")
                    .manageUrl(manageUrl)
                    .unsubscribeUrl(unsubscribeUrl)
                    .build());
        };
    }

    private MailType toMailType(NotificationType type) {
        return switch (type) {
            case DEADLINE -> MailType.DEADLINE;
            case START -> MailType.START;
            case MATURITY -> MailType.MATURITY;
        };
    }

    enum SkipReason {
        NO_EMAIL, NOT_VERIFIED, UNSUBSCRIBED
    }

    record EmailSendContext(
            Long scheduleId,
            String email,
            String subject,
            String htmlBody,
            MailType mailType,
            SkipReason skipReason
    ) {
        static EmailSendContext ready(Long scheduleId, String email, String subject, String htmlBody, MailType mailType) {
            return new EmailSendContext(scheduleId, email, subject, htmlBody, mailType, null);
        }

        static EmailSendContext skipped(Long scheduleId, SkipReason reason) {
            return new EmailSendContext(scheduleId, null, null, null, null, reason);
        }

        boolean shouldSend() {
            return skipReason == null;
        }
    }
}
