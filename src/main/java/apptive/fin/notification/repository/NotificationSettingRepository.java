package apptive.fin.notification.repository;

import apptive.fin.notification.entity.NotificationSetting;
import apptive.fin.notification.entity.NotificationType;
import apptive.fin.notification.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    List<NotificationSetting> findByUserId(Long userId);

    Optional<NotificationSetting> findByUserIdAndTargetIdAndTargetTypeAndNotificationType(
            Long userId, Long targetId, TargetType targetType, NotificationType notificationType);

    List<NotificationSetting> findByTargetIdAndTargetType(Long targetId, TargetType targetType);
}
