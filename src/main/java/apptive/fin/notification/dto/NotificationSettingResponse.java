package apptive.fin.notification.dto;

import apptive.fin.notification.entity.NotificationSetting;
import apptive.fin.notification.entity.NotificationType;
import apptive.fin.notification.entity.SettingStatus;
import apptive.fin.notification.entity.TargetType;

import java.util.List;

public record NotificationSettingResponse(
        Long id,
        Long targetId,
        TargetType targetType,
        NotificationType notificationType,
        SettingStatus status,
        List<ScheduleInfo> schedules
) {
    public record ScheduleInfo(
            Long id,
            String sendDate,
            String offsetLabel,
            String status
    ) {}

    public static NotificationSettingResponse from(NotificationSetting setting) {
        List<ScheduleInfo> scheduleInfos = setting.getSchedules().stream()
                .map(s -> new ScheduleInfo(
                        s.getId(),
                        s.getSendDate().toString(),
                        s.getOffsetLabel(),
                        s.getStatus().name()
                ))
                .toList();

        return new NotificationSettingResponse(
                setting.getId(),
                setting.getTargetId(),
                setting.getTargetType(),
                setting.getNotificationType(),
                setting.getStatus(),
                scheduleInfos
        );
    }
}
