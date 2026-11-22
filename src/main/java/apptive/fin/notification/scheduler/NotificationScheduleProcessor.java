package apptive.fin.notification.scheduler;

import apptive.fin.notification.adapter.EmailSendPort;
import apptive.fin.notification.dto.EmailSendRequest;
import apptive.fin.notification.dto.EmailSendResult;
import apptive.fin.notification.entity.*;
import apptive.fin.notification.repository.EmailSendLogRepository;
import apptive.fin.notification.repository.NotificationScheduleRepository;
import apptive.fin.notification.service.EmailTemplateService;
import apptive.fin.notification.service.UnsubscribeService;
import apptive.fin.search.repository.ProductRepository;
import apptive.fin.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processScheduleInTransaction(Long scheduleId) {
        NotificationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        processSchedule(schedule);
    }

    private void processSchedule(NotificationSchedule schedule) {
        NotificationSetting setting = schedule.getSetting();
        User user = setting.getUser();
        String email = user.getEmail();

        if (email == null || email.isBlank()) {
            log.warn("User {} has no email, skipping notification", user.getId());
            schedule.markAsSkipped();
            return;
        }

        if (!user.isEmailVerified()) {
            log.warn("User {} email not verified, skipping notification", user.getId());
            schedule.markAsSkipped();
            return;
        }

        if (unsubscribeService.isUnsubscribed(email)) {
            log.info("Email {} is unsubscribed, skipping notification", email);
            schedule.markAsSkipped();
            return;
        }

        String unsubscribeUrl = unsubscribeService.generateUnsubscribeUrl(email);
        String productName = getProductName(setting.getTargetId());
        int dday = parseDday(schedule.getOffsetLabel());
        String subject = buildSubject(setting.getNotificationType(), productName, dday);
        String htmlBody = buildHtmlBody(setting, productName, schedule.getOffsetLabel(), unsubscribeUrl);
        MailType mailType = toMailType(setting.getNotificationType());

        EmailSendRequest request = EmailSendRequest.of(email, subject, htmlBody, mailType);
        EmailSendResult result = emailSendPort.send(request);

        sendLogRepository.save(EmailSendLog.builder()
                .schedule(schedule)
                .toEmail(email)
                .mailType(mailType)
                .resendMessageId(result.messageId())
                .status(result.success() ? EmailSendStatus.SENT : EmailSendStatus.FAILED)
                .errorMessage(result.errorMessage())
                .build());

        if (result.success()) {
            schedule.markAsSent();
            log.info("Notification sent to {} for schedule {}", email, schedule.getId());
        }
    }

    private String buildSubject(NotificationType type, String productName, int dday) {
        return switch (type) {
            case DEADLINE -> templateService.buildDeadlineSubject(productName, dday);
            case START -> templateService.buildStartSubject(productName);
            case MATURITY -> templateService.buildMaturitySubject(productName, dday);
        };
    }

    private String getProductName(Long targetId) {
        return productRepository.findById(targetId)
                .map(product -> product.getProductName())
                .orElse("상품");
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

    private String buildHtmlBody(NotificationSetting setting, String productName, String offsetLabel, String unsubscribeUrl) {
        String detailUrl = "https://y-fin.com/products/" + setting.getTargetId();

        return switch (setting.getNotificationType()) {
            case DEADLINE -> templateService.buildDeadlineEmail(productName, offsetLabel, detailUrl, unsubscribeUrl);
            case START -> templateService.buildStartEmail(productName, detailUrl, unsubscribeUrl);
            case MATURITY -> templateService.buildMaturityEmail(productName, offsetLabel, unsubscribeUrl);
        };
    }

    private MailType toMailType(NotificationType type) {
        return switch (type) {
            case DEADLINE -> MailType.DEADLINE;
            case START -> MailType.START;
            case MATURITY -> MailType.MATURITY;
        };
    }
}
