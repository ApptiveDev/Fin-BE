package apptive.fin.notification.entity;

import apptive.fin.global.entity.BaseTimeEntity;
import apptive.fin.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notification_setting",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "target_id", "target_type", "notification_type"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettingStatus status = SettingStatus.ACTIVE;

    @OneToMany(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationSchedule> schedules = new ArrayList<>();

    @Builder
    public NotificationSetting(User user, Long targetId, TargetType targetType, NotificationType notificationType) {
        this.user = user;
        this.targetId = targetId;
        this.targetType = targetType;
        this.notificationType = notificationType;
    }

    public void activate() {
        this.status = SettingStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = SettingStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == SettingStatus.ACTIVE;
    }
}
