package apptive.fin.notification.controller;

import apptive.fin.notification.dto.NotificationSettingRequest;
import apptive.fin.notification.dto.NotificationSettingResponse;
import apptive.fin.notification.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notification Settings", description = "알림 설정 API")
@RestController
@RequestMapping("/notifications/settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @Operation(summary = "알림 설정 등록", description = "새로운 알림 설정을 등록하고 스케줄을 생성합니다. 마감 7일·3일·1일 전에 알림이 발송되며, 이미 지난 시점은 건너뜁니다.")
    @PostMapping
    public ResponseEntity<NotificationSettingResponse> createSetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NotificationSettingRequest request
    ) {
        NotificationSettingResponse response = notificationSettingService.createSetting(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "알림 설정 목록 조회", description = "사용자의 모든 알림 설정을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<NotificationSettingResponse>> getSettings(
            @AuthenticationPrincipal Long userId
    ) {
        List<NotificationSettingResponse> responses = notificationSettingService.getSettings(userId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "알림 설정 상세 조회", description = "특정 알림 설정의 상세 정보를 조회합니다.")
    @GetMapping("/{settingId}")
    public ResponseEntity<NotificationSettingResponse> getSetting(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long settingId
    ) {
        NotificationSettingResponse response = notificationSettingService.getSetting(userId, settingId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "알림 설정 삭제", description = "알림 설정과 관련 스케줄을 삭제합니다.")
    @DeleteMapping("/{settingId}")
    public ResponseEntity<Void> deleteSetting(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long settingId
    ) {
        notificationSettingService.deleteSetting(userId, settingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 설정 토글", description = "알림 설정의 활성화/비활성화 상태를 토글합니다.")
    @PatchMapping("/{settingId}/toggle")
    public ResponseEntity<NotificationSettingResponse> toggleSetting(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long settingId
    ) {
        NotificationSettingResponse response = notificationSettingService.toggleSetting(userId, settingId);
        return ResponseEntity.ok(response);
    }
}
