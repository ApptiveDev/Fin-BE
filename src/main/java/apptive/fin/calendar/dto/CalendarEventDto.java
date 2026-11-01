package apptive.fin.calendar.dto;

import java.time.LocalDate;

/** 캘린더 그리드에 찍히는 일정 하나. 한 회차에서 시작·마감 일정이 따로 나온다. */
public record CalendarEventDto(
        LocalDate date,
        CalendarEventType type,
        CalendarRoundDto round
) {
}
