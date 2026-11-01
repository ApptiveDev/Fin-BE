package apptive.fin.calendar.service;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.calendar.CalendarErrorCode;
import apptive.fin.calendar.dto.CalendarEventDto;
import apptive.fin.calendar.dto.CalendarEventsResponseDto;
import apptive.fin.calendar.dto.CalendarRoundDto;
import apptive.fin.calendar.dto.CalendarSummaryResponseDto;
import apptive.fin.calendar.dto.ScheduleKind;
import apptive.fin.calendar.entity.ApplyPeriodType;
import apptive.fin.calendar.repository.PolicyProductRoundRepository;
import apptive.fin.calendar.repository.YouthPolicyRepository;
import apptive.fin.calendar.repository.YouthPolicyRoundRepository;
import apptive.fin.category.entity.CategoryOption;
import apptive.fin.category.repository.CategoryOptionRepository;
import apptive.fin.global.error.BusinessException;
import apptive.fin.global.util.AgeUtil;
import apptive.fin.user.entity.UserProfile;
import apptive.fin.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * B1 캘린더 조회. 1계층(Y-Fin 정책상품 회차)과 2계층(온통청년 정책 회차)을 한 목록으로 돌려준다.
 * 필터(신청 기간 유형, 상품 분류, 내 조건 맞춤)는 FE가 응답 필드로 거른다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String REGION_CODE_PREFIX = "REGION_";

    // 같은 날짜 안에서는 마감을 먼저 두고, 같은 유형이면 1계층을 먼저 둔다(PRD 칸 안 정렬).
    private static final Comparator<CalendarEventDto> EVENT_ORDER = Comparator
            .comparing(CalendarEventDto::date)
            .thenComparing(event -> event.type().ordinal(), Comparator.reverseOrder())
            .thenComparing(event -> event.round().tier())
            .thenComparing(event -> event.round().name());

    // 지금 신청 가능: 마감 가까운 순
    private static final Comparator<CalendarRoundDto> OPEN_NOW_ORDER = Comparator
            .comparing(CalendarRoundDto::applyEndDate)
            .thenComparing(CalendarRoundDto::tier)
            .thenComparing(CalendarRoundDto::name);

    // PRD는 '최근 공고순'이지만 공고일을 저장하지 않아 최근 시작일 순으로 둔다.
    private static final Comparator<CalendarRoundDto> YEAR_ROUND_ORDER = Comparator
            .comparing(CalendarRoundDto::applyStartDate, Comparator.reverseOrder())
            .thenComparing(CalendarRoundDto::tier)
            .thenComparing(CalendarRoundDto::name);

    private final YouthPolicyRepository youthPolicyRepository;
    private final YouthPolicyRoundRepository youthPolicyRoundRepository;
    private final PolicyProductRoundRepository policyProductRoundRepository;
    private final UserProfileRepository userProfileRepository;
    private final CategoryOptionRepository categoryOptionRepository;

    public CalendarEventsResponseDto getEvents(LocalDate from, LocalDate to, AuthUserDetails user) {
        if (from.isAfter(to)) {
            throw new BusinessException(CalendarErrorCode.INVALID_DATE_RANGE);
        }
        LocalDate today = LocalDate.now(KST);
        Instant now = Instant.now();
        CalendarViewer viewer = viewerOf(user, today);

        Stream<CalendarRoundDto> rounds = Stream.concat(
                policyProductRoundRepository.findRoundsWithDateBetween(from, to).stream()
                        .map(round -> CalendarRoundMapper.fromProductRound(round, viewer, now)),
                youthPolicyRoundRepository.findActiveRoundsWithDateBetween(from, to).stream()
                        .map(round -> CalendarRoundMapper.fromYouthRound(round, viewer, now)));
        List<CalendarEventDto> events = rounds
                .flatMap(round -> eventsInRange(round, from, to, today))
                .sorted(EVENT_ORDER)
                .toList();

        return new CalendarEventsResponseDto(youthPolicyRepository.findLastSyncedAt(), events);
    }

    public CalendarSummaryResponseDto getSummary(AuthUserDetails user) {
        LocalDate today = LocalDate.now(KST);
        Instant now = Instant.now();
        CalendarViewer viewer = viewerOf(user, today);

        List<CalendarRoundDto> openRounds = Stream.concat(
                policyProductRoundRepository.findRoundsOpenOn(today).stream()
                        .map(round -> CalendarRoundMapper.fromProductRound(round, viewer, now)),
                youthPolicyRoundRepository.findActiveRoundsOpenOn(today).stream()
                        .map(round -> CalendarRoundMapper.fromYouthRound(round, viewer, now))
        ).toList();
        List<CalendarRoundDto> openNow = sortedOfKind(openRounds, ScheduleKind.PERIOD, OPEN_NOW_ORDER);
        List<CalendarRoundDto> yearRound = sortedOfKind(openRounds, ScheduleKind.YEAR_ROUND, YEAR_ROUND_ORDER);
        // 상시 신청은 2계층에만 있다. 1계층은 회차가 있는 상품만 캘린더에 나온다.
        List<CalendarRoundDto> always = youthPolicyRepository.findByActiveTrueAndApplyPeriodType(ApplyPeriodType.ALWAYS)
                .stream()
                .map(policy -> CalendarRoundMapper.fromAlwaysPolicy(policy, viewer))
                .sorted(Comparator.comparing(CalendarRoundDto::name))
                .toList();

        return new CalendarSummaryResponseDto(youthPolicyRepository.findLastSyncedAt(), openNow, yearRound, always);
    }

    // 회차 하나에서 생기는 시작·마감 일정 중 조회 기간 안에 있는 것
    private static Stream<CalendarEventDto> eventsInRange(CalendarRoundDto round, LocalDate from, LocalDate to,
                                                          LocalDate today) {
        return CalendarSchedule.eventsOf(round.applyStartDate(), round.applyEndDate(), today).stream()
                .filter(event -> !event.date().isBefore(from) && !event.date().isAfter(to))
                .map(event -> new CalendarEventDto(event.date(), event.type(), round));
    }

    private static List<CalendarRoundDto> sortedOfKind(List<CalendarRoundDto> rounds, ScheduleKind kind,
                                                       Comparator<CalendarRoundDto> order) {
        return rounds.stream()
                .filter(round -> round.scheduleKind() == kind)
                .sorted(order)
                .toList();
    }

    /** 약관 동의 전이거나 비로그인이면 null(자격을 판정하지 않음). */
    private CalendarViewer viewerOf(AuthUserDetails user, LocalDate today) {
        if (user == null || !user.getRole().canUseRecommendation()) {
            return null;
        }
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return new CalendarViewer(null, Set.of());
        }
        Set<String> regionKeys = profile.getSelectedOptionIds() == null
                ? Set.of()
                : categoryOptionRepository.findAllById(profile.getSelectedOptionIds()).stream()
                        .map(CategoryOption::getCode)
                        .filter(code -> code != null && code.startsWith(REGION_CODE_PREFIX))
                        .collect(Collectors.toSet());
        return new CalendarViewer(AgeUtil.age(profile.getBirthdate(), today), regionKeys);
    }
}
