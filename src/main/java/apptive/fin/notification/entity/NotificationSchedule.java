package apptive.fin.notification.entity;

import apptive.fin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "notification_schedule",
        uniqueConstraints = @UniqueConstraint(columnNames = {"setting_id", "send_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setting_id", nullable = false)
    private NotificationSetting setting;

    @Column(name = "send_date", nullable = false)
    private LocalDate sendDate;

    @Column(name = "offset_label", nullable = false, length = 30)
    private String offsetLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    @Builder
    public NotificationSchedule(NotificationSetting setting, LocalDate sendDate, String offsetLabel) {
        this.setting = setting;
        this.sendDate = sendDate;
        this.offsetLabel = offsetLabel;
    }

    public void markAsSent() {
        this.status = ScheduleStatus.SENT;
    }

    public void markAsSkipped() {
        this.status = ScheduleStatus.SKIPPED;
    }

    public boolean isScheduled() {
        return this.status == ScheduleStatus.SCHEDULED;
    }
}
