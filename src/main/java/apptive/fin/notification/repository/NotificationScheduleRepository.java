package apptive.fin.notification.repository;

import apptive.fin.notification.entity.NotificationSchedule;
import apptive.fin.notification.entity.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

    @Query("SELECT ns FROM NotificationSchedule ns " +
           "JOIN FETCH ns.setting s " +
           "JOIN FETCH s.user u " +
           "WHERE ns.sendDate = :date AND ns.status = :status AND s.status = 'ACTIVE'")
    List<NotificationSchedule> findByDateAndStatus(@Param("date") LocalDate date, @Param("status") ScheduleStatus status);

    List<NotificationSchedule> findBySettingId(Long settingId);
}
