package apptive.fin.calendar.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * 캘린더의 회차(일정 카드) 정보. 상시 신청 정책은 회차가 없어 roundId와 신청 기간이 null이다.
 * roundId는 계층마다 따로 매겨지므로 (tier, roundId)로 구분한다.
 *
 * @param tier             1계층(Y-Fin 정책상품)이면 productId, 2계층(온통청년 정책)이면 policyId와 externalUrl이 있다
 * @param productId        1계층 상품 ID. Y4 상세로 이동할 때 쓴다
 * @param policyId         2계층 정책 ID
 * @param institution      1계층은 상품 제공기관
 * @param summary          1계층은 상품 요약(content_summary)
 * @param regions          대상 거주지역 키(REGION_*). 전국이면 빈 목록
 * @param minAge           null이면 하한 없음
 * @param maxAge           null이면 상한 없음
 * @param externalUrl      2계층의 온통청년 정책 상세 페이지. 1계층은 null
 * @param scheduleChanged  최근 7일 안에 신청 기간이 바뀜
 * @param eligibility      비로그인(약관 동의 전 포함)이면 null
 */
@Builder
public record CalendarRoundDto(
        CalendarTier tier,
        Long productId,
        Long policyId,
        Long roundId,
        String name,
        String institution,
        String summary,
        ScheduleKind scheduleKind,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        Integer minAge,
        Integer maxAge,
        List<String> regions,
        String externalUrl,
        boolean scheduleChanged,
        Eligibility eligibility
) {
}
