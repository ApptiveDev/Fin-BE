package apptive.fin.calendar.dto;

import java.time.Instant;
import java.util.List;

/** @param asOf 온통청년 데이터를 마지막으로 수집한 시각. 수집 데이터가 없으면 null */
public record CalendarEventsResponseDto(
        Instant asOf,
        List<CalendarEventDto> events
) {
}
