package apptive.fin.search.dto;

import lombok.Builder;

/**
 * 파킹통장 상품 정보.
 * 최고금리 + 적용 범위 표시, 최고금리순 정렬.
 */
@Builder
public record ParkingProductDto(
        Long productId,
        Long productPropertyId,
        String productName,
        String providerName,
        String providerCode,

        // 금리 정보
        Double baseRate,           // 기본금리
        Double maxRate,            // 최고금리 (우대 포함)
        Long maxRateApplicableMinAmount,  // 최고금리 적용 잔액 하한 (원, 초과)
        Long maxRateApplicableMaxAmount,  // 최고금리 적용 잔액 상한 (원, 이하)
        Long maxDepositAmount,            // 최대 예치한도 (원), 적용 범위가 없을 때 표시용

        // 상품 속성
        String interestPaymentMethod,  // 이자지급방식 공시 원문 (예: 월지급)
        Boolean depositProtection,     // 예금자보호 여부

        // 아웃링크
        String applyUrl,
        String officialChannelUrl,
        String officialChannelName
) {
}
