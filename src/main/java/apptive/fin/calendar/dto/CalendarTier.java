package apptive.fin.calendar.dto;

/** PRD 데이터 계층. 같은 날 같은 유형이면 1계층이 먼저다. */
public enum CalendarTier {
    POLICY_PRODUCT, // 1계층: Y-Fin 정책상품. 상세는 Y4(productId)
    YOUTH_POLICY    // 2계층: 온통청년 청년 금융 지원 정책. 상세는 온통청년(externalUrl)
}
