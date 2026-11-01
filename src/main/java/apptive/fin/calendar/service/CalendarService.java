package apptive.fin.calendar.service;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.calendar.dto.CalendarEventDto;
import apptive.fin.calendar.dto.CalendarEventsResponseDto;
import apptive.fin.calendar.dto.CalendarRoundDto;
import apptive.fin.calendar.dto.CalendarSummaryResponseDto;
import apptive.fin.calendar.dto.CalendarTier;
import apptive.fin.calendar.dto.Eligibility;
import apptive.fin.calendar.dto.ScheduleKind;
import apptive.fin.calendar.entity.ApplyPeriodType;
import apptive.fin.calendar.entity.PolicyProductRound;
import apptive.fin.calendar.entity.YouthPolicy;
import apptive.fin.calendar.entity.YouthPolicyRound;
import apptive.fin.calendar.repository.PolicyProductRoundRepository;
import apptive.fin.calendar.repository.YouthPolicyRepository;
import apptive.fin.calendar.repository.YouthPolicyRoundRepository;
import apptive.fin.category.entity.CategoryOption;
import apptive.fin.category.repository.CategoryOptionRepository;
import apptive.fin.global.util.AgeUtil;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductProperty;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
    private static final String ONTONG_DETAIL_URL =
            "https://www.youthcenter.go.kr/youthPolicy/ythPlcyTotalSearch/ythPlcyDetail/";
    private static final Duration SCHEDULE_CHANGED_BADGE_PERIOD = Duration.ofDays(7);

    // 같은 날짜 안에서는 마감을 먼저 두고, 같은 유형이면 1계층을 먼저 둔다(PRD 칸 안 정렬).
    private static final Comparator<CalendarEventDto> EVENT_ORDER = Comparator
            .comparing(CalendarEventDto::date)
            .thenComparing(event -> event.type().ordinal(), Comparator.reverseOrder())
            .thenComparing(event -> event.round().tier())
            .thenComparing(event -> event.round().name());

    private final YouthPolicyRepository youthPolicyRepository;
    private final YouthPolicyRoundRepository youthPolicyRoundRepository;
    private final PolicyProductRoundRepository policyProductRoundRepository;
    private final UserProfileRepository userProfileRepository;
    private final CategoryOptionRepository categoryOptionRepository;

    public CalendarEventsResponseDto getEvents(LocalDate from, LocalDate to, AuthUserDetails user) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
        }
        LocalDate today = LocalDate.now(KST);
        Instant now = Instant.now();
        Viewer viewer = viewerOf(user, today);

        Stream<CalendarRoundDto> rounds = Stream.concat(
                policyProductRoundRepository.findRoundsWithDateBetween(from, to).stream()
                        .map(round -> toDto(round, viewer, now)),
                youthPolicyRoundRepository.findActiveRoundsWithDateBetween(from, to).stream()
                        .map(round -> toDto(round, viewer, now)));
        List<CalendarEventDto> events = rounds
                .flatMap(round -> CalendarSchedule.eventsOf(round.applyStartDate(), round.applyEndDate(), today)
                        .stream()
                        .filter(event -> !event.date().isBefore(from) && !event.date().isAfter(to))
                        .map(event -> new CalendarEventDto(event.date(), event.type(), round)))
                .sorted(EVENT_ORDER)
                .toList();

        return new CalendarEventsResponseDto(youthPolicyRepository.findLastSyncedAt(), events);
    }

    public CalendarSummaryResponseDto getSummary(AuthUserDetails user) {
        LocalDate today = LocalDate.now(KST);
        Instant now = Instant.now();
        Viewer viewer = viewerOf(user, today);

        List<CalendarRoundDto> openRounds = Stream.concat(
                policyProductRoundRepository.findRoundsOpenOn(today).stream()
                        .map(round -> toDto(round, viewer, now)),
                youthPolicyRoundRepository.findActiveRoundsOpenOn(today).stream()
                        .map(round -> toDto(round, viewer, now))
        ).toList();

        List<CalendarRoundDto> openNow = openRounds.stream()
                .filter(round -> round.scheduleKind() == ScheduleKind.PERIOD)
                .sorted(Comparator.comparing(CalendarRoundDto::applyEndDate)
                        .thenComparing(CalendarRoundDto::tier)
                        .thenComparing(CalendarRoundDto::name))
                .toList();
        // PRD는 '최근 공고순'이지만 공고일을 저장하지 않아 최근 시작일 순으로 둔다.
        List<CalendarRoundDto> yearRound = openRounds.stream()
                .filter(round -> round.scheduleKind() == ScheduleKind.YEAR_ROUND)
                .sorted(Comparator.comparing(CalendarRoundDto::applyStartDate).reversed()
                        .thenComparing(CalendarRoundDto::tier)
                        .thenComparing(CalendarRoundDto::name))
                .toList();
        // 상시 신청은 2계층에만 있다. 1계층은 회차가 있는 상품만 캘린더에 나온다.
        List<CalendarRoundDto> always = youthPolicyRepository.findByActiveTrueAndApplyPeriodType(ApplyPeriodType.ALWAYS)
                .stream()
                .map(policy -> toAlwaysDto(policy, viewer))
                .sorted(Comparator.comparing(CalendarRoundDto::name))
                .toList();

        return new CalendarSummaryResponseDto(youthPolicyRepository.findLastSyncedAt(), openNow, yearRound, always);
    }

    private CalendarRoundDto toDto(PolicyProductRound round, Viewer viewer, Instant now) {
        Product product = round.getProduct();
        List<ProductProperty> joinable = product.getProperties().stream()
                .filter(ProductProperty::isJoinable)
                .toList();
        Integer minAge = widestBound(joinable, ProductProperty::getMinAge, Comparator.naturalOrder());
        Integer maxAge = widestBound(joinable, ProductProperty::getMaxAge, Comparator.reverseOrder());
        List<String> regions = regionsOf(joinable);
        return new CalendarRoundDto(
                CalendarTier.POLICY_PRODUCT,
                product.getId(),
                null,
                round.getId(),
                product.getDisplayProductName(),
                joinable.stream().map(ProductProperty::providerName).filter(Objects::nonNull).findFirst().orElse(null),
                product.getContentSummary(),
                CalendarSchedule.kindOf(round.getApplyStartDate(), round.getApplyEndDate()),
                round.getApplyStartDate(),
                round.getApplyEndDate(),
                minAge,
                maxAge,
                regions,
                null,
                isRecentlyChanged(round.getScheduleChangedAt(), now),
                viewer == null ? null : PolicyEligibility.judgeByRegionKeys(
                        minAge, maxAge, regions, viewer.age(), viewer.regionKeys())
        );
    }

    // 옵션(일반형·우대형 등)마다 나이 조건이 다르면 가장 넓은 범위를 쓴다. 한 옵션이라도 제한이 없으면 제한 없음.
    // 군 복무 연장 나이는 보지 않는다(plan-vs-implementation 참고).
    private static Integer widestBound(List<ProductProperty> properties,
                                       Function<ProductProperty, Integer> bound,
                                       Comparator<Integer> widerFirst) {
        if (properties.isEmpty() || properties.stream().map(bound).anyMatch(Objects::isNull)) {
            return null;
        }
        return properties.stream().map(bound).min(widerFirst).orElse(null);
    }

    // 정부상품의 지역 조건은 옵션 키워드(REGION_*)로 수집된다. 키워드가 없으면 전국.
    private static List<String> regionsOf(List<ProductProperty> properties) {
        return properties.stream()
                .flatMap(property -> property.keywordCodes().stream())
                .map(Enum::name)
                .filter(code -> code.startsWith(REGION_CODE_PREFIX))
                .distinct()
                .sorted()
                .toList();
    }

    private CalendarRoundDto toDto(YouthPolicyRound round, Viewer viewer, Instant now) {
        YouthPolicy policy = round.getPolicy();
        return new CalendarRoundDto(
                CalendarTier.YOUTH_POLICY,
                null,
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
                isRecentlyChanged(round.getScheduleChangedAt(), now),
                eligibilityOf(policy, viewer)
        );
    }

    private CalendarRoundDto toAlwaysDto(YouthPolicy policy, Viewer viewer) {
        return new CalendarRoundDto(
                CalendarTier.YOUTH_POLICY,
                null,
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

    private static boolean isRecentlyChanged(Instant scheduleChangedAt, Instant now) {
        return scheduleChangedAt != null && scheduleChangedAt.isAfter(now.minus(SCHEDULE_CHANGED_BADGE_PERIOD));
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
