package apptive.fin.notification.scheduler;

import apptive.fin.notification.entity.NotificationSchedule;
import apptive.fin.notification.entity.ScheduleStatus;
import apptive.fin.notification.repository.NotificationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationScheduleRepository scheduleRepository;
    private final NotificationScheduleProcessor scheduleProcessor;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendScheduledNotifications() {
        LocalDate today = LocalDate.now();
        log.info("Starting scheduled notification job for date: {}", today);

        List<NotificationSchedule> schedules = scheduleRepository.findByDateAndStatus(today, ScheduleStatus.SCHEDULED);
        log.info("Found {} scheduled notifications", schedules.size());

        for (NotificationSchedule schedule : schedules) {
            try {
                scheduleProcessor.processSchedule(schedule.getId());
            } catch (Exception e) {
                log.error("Failed to process schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }

        log.info("Completed scheduled notification job");
    }
}
