package apptive.fin.calendar.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 캘린더의 회차(일정 카드) 정보. 상시 신청 정책은 회차가 없어 roundId와 신청 기간이 null이다.
 *
 * @param regions          대상 거주지역 키(REGION_*). 전국이면 빈 목록
 * @param minAge           null이면 하한 없음
 * @param maxAge           null이면 상한 없음
 * @param externalUrl      온통청년 정책 상세 페이지
 * @param scheduleChanged  최근 7일 안에 신청 기간이 바뀜
 * @param eligibility      비로그인(약관 동의 전 포함)이면 null
 */
public record CalendarRoundDto(
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
