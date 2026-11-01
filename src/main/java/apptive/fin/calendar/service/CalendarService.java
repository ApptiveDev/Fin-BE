package apptive.fin.calendar.service;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.calendar.dto.CalendarEventDto;
import apptive.fin.calendar.dto.CalendarEventsResponseDto;
import apptive.fin.calendar.dto.CalendarRoundDto;
import apptive.fin.calendar.dto.CalendarSummaryResponseDto;
import apptive.fin.calendar.dto.Eligibility;
import apptive.fin.calendar.dto.ScheduleKind;
import apptive.fin.calendar.entity.ApplyPeriodType;
import apptive.fin.calendar.entity.YouthPolicy;
import apptive.fin.calendar.entity.YouthPolicyRound;
import apptive.fin.calendar.repository.YouthPolicyRepository;
import apptive.fin.calendar.repository.YouthPolicyRoundRepository;
import apptive.fin.category.entity.CategoryOption;
import apptive.fin.category.repository.CategoryOptionRepository;
import apptive.fin.global.util.AgeUtil;
import apptive.fin.user.entity.UserProfile;
import apptive.fin.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** B1 캘린더 조회. 필터(신청 기간 유형, 내 조건 맞춤)는 FE가 응답 필드로 거른다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String REGION_CODE_PREFIX = "REGION_";
    private static final String ONTONG_DETAIL_URL =
            "https://www.youthcenter.go.kr/youthPolicy/ythPlcyTotalSearch/ythPlcyDetail/";
    private static final Duration SCHEDULE_CHANGED_BADGE_PERIOD = Duration.ofDays(7);

    // 같은 날짜 안에서는 마감을 먼저 둔다(PRD 칸 안 정렬: 마감 → 시작).
    private static final Comparator<CalendarEventDto> EVENT_ORDER = Comparator
            .comparing(CalendarEventDto::date)
            .thenComparing(event -> event.type().ordinal(), Comparator.reverseOrder())
            .thenComparing(event -> event.round().name());

    private final YouthPolicyRepository youthPolicyRepository;
    private final YouthPolicyRoundRepository youthPolicyRoundRepository;
    private final UserProfileRepository userProfileRepository;
    private final CategoryOptionRepository categoryOptionRepository;

    public CalendarEventsResponseDto getEvents(LocalDate from, LocalDate to, AuthUserDetails user) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
        }
        LocalDate today = LocalDate.now(KST);
        Instant now = Instant.now();
        Viewer viewer = viewerOf(user, today);

        List<CalendarEventDto> events = youthPolicyRoundRepository.findActiveRoundsWithDateBetween(from, to).stream()
                .flatMap(round -> CalendarSchedule.eventsOf(round.getApplyStartDate(), round.getApplyEndDate(), today)
                        .stream()
                        .filter(event -> !event.date().isBefore(from) && !event.date().isAfter(to))
                        .map(event -> new CalendarEventDto(event.date(), event.type(), toDto(round, viewer, now))))
                .sorted(EVENT_ORDER)
                .toList();

        return new CalendarEventsResponseDto(youthPolicyRepository.findLastSyncedAt(), events);
    }

    public CalendarSummaryResponseDto getSummary(AuthUserDetails user) {
        LocalDate today = LocalDate.now(KST);
        Instant now = Instant.now();
        Viewer viewer = viewerOf(user, today);

        List<CalendarRoundDto> openRounds = youthPolicyRoundRepository.findActiveRoundsOpenOn(today).stream()
                .map(round -> toDto(round, viewer, now))
                .toList();

        List<CalendarRoundDto> openNow = openRounds.stream()
                .filter(round -> round.scheduleKind() == ScheduleKind.PERIOD)
                .sorted(Comparator.comparing(CalendarRoundDto::applyEndDate).thenComparing(CalendarRoundDto::name))
                .toList();
        // PRD는 '최근 공고순'이지만 공고일을 저장하지 않아 최근 시작일 순으로 둔다.
        List<CalendarRoundDto> yearRound = openRounds.stream()
                .filter(round -> round.scheduleKind() == ScheduleKind.YEAR_ROUND)
                .sorted(Comparator.comparing(CalendarRoundDto::applyStartDate).reversed()
                        .thenComparing(CalendarRoundDto::name))
                .toList();
        List<CalendarRoundDto> always = youthPolicyRepository.findByActiveTrueAndApplyPeriodType(ApplyPeriodType.ALWAYS)
                .stream()
                .map(policy -> toAlwaysDto(policy, viewer))
                .sorted(Comparator.comparing(CalendarRoundDto::name))
                .toList();

        return new CalendarSummaryResponseDto(youthPolicyRepository.findLastSyncedAt(), openNow, yearRound, always);
    }

    private CalendarRoundDto toDto(YouthPolicyRound round, Viewer viewer, Instant now) {
        YouthPolicy policy = round.getPolicy();
        boolean scheduleChanged = round.getScheduleChangedAt() != null
                && round.getScheduleChangedAt().isAfter(now.minus(SCHEDULE_CHANGED_BADGE_PERIOD));
        return new CalendarRoundDto(
                policy.getId(),
                round.getId(),
                policy.getName(),
                policy.getInstitution(),
                policy.getSummary(),
                CalendarSchedule.kindOf(round.getApplyStartDate(), round.getApplyEndDate()),
                round.getApplyStartDate(),
                round.getApplyEndDate(),
                policy.getMinAge(),
                policy.getMaxAge(),
                PolicyEligibility.regionsOf(policy.getRegionCodes()),
                ONTONG_DETAIL_URL + policy.getPolicyNo(),
                scheduleChanged,
                eligibilityOf(policy, viewer)
        );
    }

    private CalendarRoundDto toAlwaysDto(YouthPolicy policy, Viewer viewer) {
        return new CalendarRoundDto(
                policy.getId(),
                null,
                policy.getName(),
                policy.getInstitution(),
                policy.getSummary(),
                ScheduleKind.ALWAYS,
                null,
                null,
                policy.getMinAge(),
                policy.getMaxAge(),
                PolicyEligibility.regionsOf(policy.getRegionCodes()),
                ONTONG_DETAIL_URL + policy.getPolicyNo(),
                false,
                eligibilityOf(policy, viewer)
        );
    }

    private Eligibility eligibilityOf(YouthPolicy policy, Viewer viewer) {
        if (viewer == null) {
            return null;
        }
        return PolicyEligibility.judge(
                policy.getMinAge(), policy.getMaxAge(), policy.getRegionCodes(), viewer.age(), viewer.regionKeys());
    }

    /** 자격 판정에 쓰는 로그인 사용자 정보. 약관 동의 전이거나 비로그인이면 null(판정하지 않음). */
    private Viewer viewerOf(AuthUserDetails user, LocalDate today) {
        if (user == null || !user.getRole().canUseRecommendation()) {
            return null;
        }
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return new Viewer(null, Set.of());
        }
        Set<String> regionKeys = profile.getSelectedOptionIds() == null
                ? Set.of()
                : categoryOptionRepository.findAllById(profile.getSelectedOptionIds()).stream()
                        .map(CategoryOption::getCode)
                        .filter(code -> code != null && code.startsWith(REGION_CODE_PREFIX))
                        .collect(Collectors.toSet());
        return new Viewer(AgeUtil.age(profile.getBirthdate(), today), regionKeys);
    }

    private record Viewer(Integer age, Set<String> regionKeys) {
    }
}
