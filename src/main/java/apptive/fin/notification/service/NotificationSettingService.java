package apptive.fin.notification.service;

import apptive.fin.global.error.BusinessException;
import apptive.fin.notification.NotificationErrorCode;
import apptive.fin.notification.dto.NotificationSettingRequest;
import apptive.fin.notification.dto.NotificationSettingResponse;
import apptive.fin.notification.entity.NotificationSchedule;
import apptive.fin.notification.entity.NotificationSetting;
import apptive.fin.notification.entity.NotificationType;
import apptive.fin.notification.repository.NotificationScheduleRepository;
import apptive.fin.notification.repository.NotificationSettingRepository;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private static final List<Integer> DEADLINE_OFFSETS = List.of(7, 3, 1);

    private final NotificationSettingRepository settingRepository;
    private final NotificationScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public NotificationSettingResponse createSetting(Long userId, NotificationSettingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.USER_NOT_FOUND));

        // 중복 체크
        settingRepository.findByUserIdAndTargetIdAndTargetTypeAndNotificationType(
                userId, request.targetId(), request.targetType(), request.notificationType()
        ).ifPresent(existing -> {
            throw new BusinessException(NotificationErrorCode.SETTING_ALREADY_EXISTS);
        });

        // 마감 알림인 경우 마감일 검증
        if (request.notificationType() == NotificationType.DEADLINE) {
            validateDeadlineDate(request.deadlineDate());
        }

        // 알림 설정 생성
        NotificationSetting setting = NotificationSetting.builder()
                .user(user)
                .targetId(request.targetId())
                .targetType(request.targetType())
                .notificationType(request.notificationType())
                .build();

        settingRepository.save(setting);

        // 스케줄 생성
        if (request.notificationType() == NotificationType.DEADLINE && request.deadlineDate() != null) {
            createDeadlineSchedules(setting, request.deadlineDate());
        }

        log.info("Created notification setting {} for user {}", setting.getId(), userId);
        return NotificationSettingResponse.from(setting);
    }

    @Transactional(readOnly = true)
    public List<NotificationSettingResponse> getSettings(Long userId) {
        return settingRepository.findByUserId(userId).stream()
                .map(NotificationSettingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationSettingResponse getSetting(Long userId, Long settingId) {
        NotificationSetting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.SETTING_NOT_FOUND));

        if (!setting.getUser().getId().equals(userId)) {
            throw new BusinessException(NotificationErrorCode.SETTING_NOT_FOUND);
        }

        return NotificationSettingResponse.from(setting);
    }

    @Transactional
    public void deleteSetting(Long userId, Long settingId) {
        NotificationSetting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.SETTING_NOT_FOUND));

        if (!setting.getUser().getId().equals(userId)) {
            throw new BusinessException(NotificationErrorCode.SETTING_NOT_FOUND);
        }

        settingRepository.delete(setting);
        log.info("Deleted notification setting {} for user {}", settingId, userId);
    }

    @Transactional
    public NotificationSettingResponse toggleSetting(Long userId, Long settingId) {
        NotificationSetting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.SETTING_NOT_FOUND));

        if (!setting.getUser().getId().equals(userId)) {
            throw new BusinessException(NotificationErrorCode.SETTING_NOT_FOUND);
        }

        if (setting.isActive()) {
            setting.deactivate();
        } else {
            setting.activate();
        }

        log.info("Toggled notification setting {} to {} for user {}", settingId, setting.getStatus(), userId);
        return NotificationSettingResponse.from(setting);
    }

    /**
     * 마감 알림 스케줄 생성 (D-7, D-3, D-1)
     * 이미 지난 날짜는 건너뜀
     */
    private void createDeadlineSchedules(NotificationSetting setting, LocalDate deadlineDate) {
        LocalDate today = LocalDate.now();

        DEADLINE_OFFSETS.stream()
                .map(offset -> new ScheduleCandidate(offset, deadlineDate.minusDays(offset)))
                .filter(candidate -> candidate.sendDate().isAfter(today))  // 이미 지난 시점 건너뛰기
                .forEach(candidate -> {
                    NotificationSchedule schedule = NotificationSchedule.builder()
                            .setting(setting)
                            .sendDate(candidate.sendDate())
                            .offsetLabel("D-" + candidate.offset())
                            .build();
                    scheduleRepository.save(schedule);
                    setting.getSchedules().add(schedule);
                    log.debug("Created schedule for D-{} on {}", candidate.offset(), candidate.sendDate());
                });

        log.info("Created {} schedules for setting {}", setting.getSchedules().size(), setting.getId());
    }

    private void validateDeadlineDate(LocalDate deadlineDate) {
        if (deadlineDate == null) {
            throw new BusinessException(NotificationErrorCode.INVALID_DEADLINE_DATE);
        }
        if (!deadlineDate.isAfter(LocalDate.now())) {
            throw new BusinessException(NotificationErrorCode.INVALID_DEADLINE_DATE);
        }
    }

    private record ScheduleCandidate(int offset, LocalDate sendDate) {}
}
