package apptive.fin.notification.scheduler;

import apptive.fin.notification.adapter.EmailSendPort;
import apptive.fin.notification.dto.EmailSendRequest;
import apptive.fin.notification.dto.EmailSendResult;
import apptive.fin.notification.entity.*;
import apptive.fin.notification.repository.EmailSendLogRepository;
import apptive.fin.notification.repository.NotificationGlobalRepository;
import apptive.fin.notification.repository.NotificationScheduleRepository;
import apptive.fin.notification.service.EmailTemplateService;
import apptive.fin.notification.service.UnsubscribeService;
import apptive.fin.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationScheduleRepository scheduleRepository;
    private final NotificationGlobalRepository globalRepository;
    private final EmailSendLogRepository sendLogRepository;
    private final EmailSendPort emailSendPort;
    private final EmailTemplateService templateService;
    private final UnsubscribeService unsubscribeService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendScheduledNotifications() {
        LocalDate today = LocalDate.now();
        log.info("Starting scheduled notification job for date: {}", today);

        List<NotificationSchedule> schedules = scheduleRepository.findByDateAndStatus(today, ScheduleStatus.SCHEDULED);
        log.info("Found {} scheduled notifications", schedules.size());

        for (NotificationSchedule schedule : schedules) {
            try {
                processSchedule(schedule);
            } catch (Exception e) {
                log.error("Failed to process schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }

        log.info("Completed scheduled notification job");
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
        String subject = buildSubject(setting.getNotificationType(), schedule.getOffsetLabel());
        String htmlBody = buildHtmlBody(setting, schedule.getOffsetLabel(), unsubscribeUrl);
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

    private String buildSubject(NotificationType type, String offsetLabel) {
        return switch (type) {
            case DEADLINE -> "[Y-Fin.] " + offsetLabel + " 마감 알림";
            case START -> "[Y-Fin.] 신청 시작 알림";
            case MATURITY -> "[Y-Fin.] " + offsetLabel + " 만기 알림";
        };
    }

    private String buildHtmlBody(NotificationSetting setting, String offsetLabel, String unsubscribeUrl) {
        // TODO: 실제 상품명 조회 로직 추가 필요
        String productName = "상품명";
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
