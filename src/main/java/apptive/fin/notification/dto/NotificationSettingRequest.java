package apptive.fin.notification.dto;

import apptive.fin.notification.entity.NotificationType;
import apptive.fin.notification.entity.TargetType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record NotificationSettingRequest(
        @NotNull Long targetId,
        @NotNull TargetType targetType,
        @NotNull NotificationType notificationType,
        LocalDate deadlineDate  // DEADLINE 타입일 때 필수
) {}
