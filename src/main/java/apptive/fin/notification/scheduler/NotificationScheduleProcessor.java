package apptive.fin.notification.scheduler;

import apptive.fin.notification.adapter.EmailSendPort;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduleProcessor {

    private final NotificationScheduleRepository scheduleRepository;
    private final EmailSendLogRepository sendLogRepository;
    private final EmailSendPort emailSendPort;
    private final EmailTemplateService templateService;
    private final UnsubscribeService unsubscribeService;
    private final ProductRepository productRepository;

    /**
     * 스케줄 처리 메인 메서드 (트랜잭션 분리)
     *
     * [TX 1] SCHEDULED → SENDING 상태 변경으로 작업 선점
     * [트랜잭션 없음] 외부 서비스(이메일) 호출
     * [TX 2] 결과에 따라 SENT/FAILED/SKIPPED 상태 변경 및 로그 저장
     */
    public void processSchedule(Long scheduleId) {
        // [TX 1] 작업 선점 - SCHEDULED → SENDING
        EmailSendContext context = claimSchedule(scheduleId);
        if (context == null) {
            return; // 이미 다른 프로세스가 처리 중이거나 스킵됨
        }

        // [트랜잭션 없음] 이메일 전송
        EmailSendResult result = null;
        if (context.shouldSend()) {
            result = sendEmail(context);
        }

        // [TX 2] 결과 저장
        completeSchedule(scheduleId, context, result);
    }

    @Transactional
    protected EmailSendContext claimSchedule(Long scheduleId) {
        // 조건부 UPDATE로 작업 선점
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

        // 스킵 조건 체크
        SkipReason skipReason = checkSkipConditions(user, email);
        if (skipReason != null) {
            return EmailSendContext.skipped(scheduleId, skipReason);
        }

        // 이메일 전송에 필요한 정보 수집
        String unsubscribeUrl = unsubscribeService.generateUnsubscribeUrl(email);
        Product product = getProduct(setting.getTargetId());
        String productName = product != null ? product.getDisplayProductName() : "상품";
        int dday = parseDday(schedule.getOffsetLabel());
        String subject = buildSubject(setting.getNotificationType(), productName, dday);
        String htmlBody = buildHtmlBody(setting, product, dday, unsubscribeUrl);
        MailType mailType = toMailType(setting.getNotificationType());

        return EmailSendContext.ready(scheduleId, email, subject, htmlBody, mailType);
    }

    private EmailSendResult sendEmail(EmailSendContext context) {
        EmailSendRequest request = EmailSendRequest.of(
                context.email(), context.subject(), context.htmlBody(), context.mailType());
        return emailSendPort.send(request);
    }

    @Transactional
    protected void completeSchedule(Long scheduleId, EmailSendContext context, EmailSendResult result) {
        NotificationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        if (context.skipReason() != null) {
            schedule.markAsSkipped();
            log.info("Schedule {} skipped: {}", scheduleId, context.skipReason());
            return;
        }

        // 발송 로그 저장
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
            case START -> templateService.buildStartSubject(productName, dday == 0);  // D-0: 당일, D-1: 전날
            case MATURITY -> templateService.buildMaturitySubject(productName, dday);
        };
    }

    private Product getProduct(Long targetId) {
        return productRepository.findById(targetId).orElse(null);
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
                    .isToday(dday == 0)  // D-0: 당일, D-1: 전날
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

    private enum SkipReason {
        NO_EMAIL, NOT_VERIFIED, UNSUBSCRIBED
    }

    private record EmailSendContext(
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
