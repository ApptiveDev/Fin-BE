package apptive.fin.calendar.service;

import apptive.fin.calendar.dto.CalendarEventType;
import apptive.fin.calendar.dto.ScheduleKind;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 회차의 일정 유형과 캘린더에 찍을 일정을 정한다.
 * 규칙 원본: docs/planning/b1-government-calendar.md 「일정 유형과 판정 규칙」
 */
public final class CalendarSchedule {

    private static final int YEAR_ROUND_MIN_DAYS = 180;
    private static final MonthDay YEAR_END = MonthDay.of(12, 31);

    private CalendarSchedule() {
    }

    public record Event(CalendarEventType type, LocalDate date) {
    }

    // 기간 일수는 양 끝을 포함한다(PRD 예시: 09.01~12.31 = 122일).
    public static ScheduleKind kindOf(LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        boolean endsAtYearEnd = MonthDay.from(end).equals(YEAR_END);
        return days >= YEAR_ROUND_MIN_DAYS && endsAtYearEnd ? ScheduleKind.YEAR_ROUND : ScheduleKind.PERIOD;
    }

    public static List<Event> eventsOf(LocalDate start, LocalDate end, LocalDate today) {
        if (kindOf(start, end) == ScheduleKind.YEAR_ROUND) {
            // 연중 접수는 마감 칩이 없다. 아직 시작 전이면 시작 칩만 찍는다.
            return start.isAfter(today) ? List.of(new Event(CalendarEventType.START, start)) : List.of();
        }
        if (start.equals(end)) {
            return List.of(new Event(CalendarEventType.END, end));
        }
        return List.of(new Event(CalendarEventType.START, start), new Event(CalendarEventType.END, end));
    }

    public static boolean isOpen(LocalDate start, LocalDate end, LocalDate today) {
        return !today.isBefore(start) && !today.isAfter(end);
    }
}
