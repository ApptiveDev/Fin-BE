package apptive.fin.calendar.dto;

import java.time.Instant;
import java.util.List;

/**
 * 보고 있는 월과 무관한 오늘 기준 정보.
 *
 * @param openNow   오늘 신청 가능한 회차(연중 접수 제외). 마감이 가까운 순
 * @param yearRound 오늘 접수 중인 연중 접수 회차
 * @param always    상시 신청 정책
 */
public record CalendarSummaryResponseDto(
        Instant asOf,
        List<CalendarRoundDto> openNow,
        List<CalendarRoundDto> yearRound,
        List<CalendarRoundDto> always
) {
}
