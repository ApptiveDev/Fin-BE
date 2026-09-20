package apptive.fin.search.dto;

import apptive.fin.search.enums.KeywordValueEnum;
import apptive.fin.search.enums.ProductType;

import java.util.List;

/**
 * 추천 랭킹이 선택한 옵션 하나를 목록 카드에 표시하기 위한 경량 요약.
 * achievableRate는 은행 금리 또는 정부 기여금 환산 수익률이며 금리 탭이 잠기면 null이다.
 */
public record ProductCardSummaryDto(
        Long productId,
        Long productPropertyId,
        ProductType productType,
        List<KeywordValueEnum> badgeKeywords,
        List<Integer> saveTrms,
        // 적금 전용: 월 최소/최대 납입액 (예금이면 null)
        Long minMonthlyLimit,
        Long maxMonthlyLimit,
        // 예금 전용: 최소/최대 예치가능금액 (적금이면 null)
        Long minDepositAmount,
        Long maxDepositAmount,
        Double matchScore,
        Double baseRate,
        Double maxRate,
        Double achievableRate,
        Long expectedTotalContribution,
        Long effectiveMonthlyDeposit,
        Integer contributionPeriodMonths
) {
}
