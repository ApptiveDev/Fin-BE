package apptive.fin.calendar.service;

import apptive.fin.calendar.dto.CalendarRoundDto;
import apptive.fin.calendar.dto.CalendarTier;
import apptive.fin.calendar.dto.Eligibility;
import apptive.fin.calendar.dto.ScheduleKind;
import apptive.fin.calendar.entity.PolicyProductRound;
import apptive.fin.calendar.entity.YouthPolicy;
import apptive.fin.calendar.entity.YouthPolicyRound;
import apptive.fin.search.entity.Product;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** 캘린더 회차·정책 엔티티를 일정 카드(CalendarRoundDto)로 바꾼다. viewer가 null이면 자격을 판정하지 않는다. */
final class CalendarRoundMapper {

    private static final String ONTONG_DETAIL_URL =
            "https://www.youthcenter.go.kr/youthPolicy/ythPlcyTotalSearch/ythPlcyDetail/";
    private static final Duration SCHEDULE_CHANGED_BADGE_PERIOD = Duration.ofDays(7);

    private CalendarRoundMapper() {
    }

    /** 1계층: Y-Fin 정책상품 회차. 상세는 Y4(productId). */
    static CalendarRoundDto fromProductRound(PolicyProductRound round, CalendarViewer viewer, Instant now) {
        Product product = round.getProduct();
        ProductConditions conditions = ProductConditions.of(product);
        return CalendarRoundDto.builder()
                .tier(CalendarTier.POLICY_PRODUCT)
                .productId(product.getId())
                .roundId(round.getId())
                .name(product.getDisplayProductName())
                .institution(conditions.institution())
                .summary(product.getContentSummary())
                .scheduleKind(CalendarSchedule.kindOf(round.getApplyStartDate(), round.getApplyEndDate()))
                .applyStartDate(round.getApplyStartDate())
                .applyEndDate(round.getApplyEndDate())
                .minAge(conditions.minAge())
                .maxAge(conditions.maxAge())
                .regions(conditions.regions())
                .scheduleChanged(isRecentlyChanged(round.getScheduleChangedAt(), now))
                .eligibility(judge(conditions.minAge(), conditions.maxAge(), conditions.regions(), viewer))
                .build();
    }

    /** 2계층: 온통청년 정책 회차. 상세는 온통청년(externalUrl). */
    static CalendarRoundDto fromYouthRound(YouthPolicyRound round, CalendarViewer viewer, Instant now) {
        YouthPolicy policy = round.getPolicy();
        return youthPolicyCard(policy, viewer)
                .roundId(round.getId())
                .scheduleKind(CalendarSchedule.kindOf(round.getApplyStartDate(), round.getApplyEndDate()))
                .applyStartDate(round.getApplyStartDate())
                .applyEndDate(round.getApplyEndDate())
                .scheduleChanged(isRecentlyChanged(round.getScheduleChangedAt(), now))
                .build();
    }

    /** 2계층 상시 신청 정책. 회차와 신청 기간이 없다. */
    static CalendarRoundDto fromAlwaysPolicy(YouthPolicy policy, CalendarViewer viewer) {
        return youthPolicyCard(policy, viewer)
                .scheduleKind(ScheduleKind.ALWAYS)
                .build();
    }

    private static CalendarRoundDto.CalendarRoundDtoBuilder youthPolicyCard(YouthPolicy policy, CalendarViewer viewer) {
        List<String> regions = PolicyEligibility.regionsOf(policy.getRegionCodes());
        return CalendarRoundDto.builder()
                .tier(CalendarTier.YOUTH_POLICY)
                .policyId(policy.getId())
                .name(policy.getName())
                .institution(policy.getInstitution())
                .summary(policy.getSummary())
                .minAge(policy.getMinAge())
                .maxAge(policy.getMaxAge())
                .regions(regions)
                .externalUrl(ONTONG_DETAIL_URL + policy.getPolicyNo())
                .eligibility(judge(policy.getMinAge(), policy.getMaxAge(), regions, viewer));
    }

    private static Eligibility judge(Integer minAge, Integer maxAge, List<String> regions, CalendarViewer viewer) {
        if (viewer == null) {
            return null;
        }
        return PolicyEligibility.judge(minAge, maxAge, regions, viewer.age(), viewer.regionKeys());
    }

    private static boolean isRecentlyChanged(Instant scheduleChangedAt, Instant now) {
        return scheduleChangedAt != null && scheduleChangedAt.isAfter(now.minus(SCHEDULE_CHANGED_BADGE_PERIOD));
    }
}
