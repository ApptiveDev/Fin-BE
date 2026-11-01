package apptive.fin.calendar;

import apptive.fin.calendar.dto.CalendarEventType;
import apptive.fin.calendar.dto.ScheduleKind;
import apptive.fin.calendar.service.CalendarSchedule;
import apptive.fin.calendar.service.CalendarSchedule.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 기준: docs/planning/b1-government-calendar.md 「일정 유형과 판정 규칙」
class CalendarScheduleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Test
    void 짧은_회차는_시작과_마감_일정을_만든다() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 8);

        assertThat(CalendarSchedule.kindOf(start, end)).isEqualTo(ScheduleKind.PERIOD);
        assertThat(CalendarSchedule.eventsOf(start, end, TODAY)).containsExactly(
                new Event(CalendarEventType.START, start),
                new Event(CalendarEventType.END, end));
    }

    @Test
    void 시작일과_종료일이_같으면_마감_일정만_만든다() {
        LocalDate day = LocalDate.of(2026, 11, 20);

        assertThat(CalendarSchedule.eventsOf(day, day, TODAY))
                .containsExactly(new Event(CalendarEventType.END, day));
    }

    @Test
    void 일년_내내_받는_회차는_연중_접수다() {
        assertThat(CalendarSchedule.kindOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .isEqualTo(ScheduleKind.YEAR_ROUND);
    }

    // PRD 예시: 09.01~12.31은 122일이라 180일 미만 → 마감 칩을 찍는다.
    @Test
    void 연말_종료여도_180일_미만이면_일반_회차다() {
        assertThat(CalendarSchedule.kindOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31)))
                .isEqualTo(ScheduleKind.PERIOD);
    }

    // 기간 일수는 양 끝을 포함해 센다(PRD 예시 09.01~12.31 = 122일). 07.05~12.31이 정확히 180일.
    @Test
    void 연중_접수_경계는_양_끝_포함_180일이다() {
        LocalDate end = LocalDate.of(2026, 12, 31);

        assertThat(CalendarSchedule.kindOf(LocalDate.of(2026, 7, 5), end)).isEqualTo(ScheduleKind.YEAR_ROUND);
        assertThat(CalendarSchedule.kindOf(LocalDate.of(2026, 7, 6), end)).isEqualTo(ScheduleKind.PERIOD);
    }

    @Test
    void 기간이_180일_이상이어도_연말에_끝나지_않으면_일반_회차다() {
        assertThat(CalendarSchedule.kindOf(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 11, 30)))
                .isEqualTo(ScheduleKind.PERIOD);
    }

    @Test
    void 연중_접수는_이미_시작했으면_일정을_만들지_않는다() {
        assertThat(CalendarSchedule.eventsOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), TODAY))
                .isEmpty();
    }

    @Test
    void 연중_접수는_시작일이_미래면_시작_일정만_만든다() {
        LocalDate start = LocalDate.of(2027, 1, 1);

        assertThat(CalendarSchedule.eventsOf(start, LocalDate.of(2027, 12, 31), TODAY))
                .containsExactly(new Event(CalendarEventType.START, start));
    }

    @Test
    void 지금_신청_가능은_시작일과_종료일을_포함한다() {
        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2026, 11, 1);

        assertThat(CalendarSchedule.isOpen(start, end, TODAY)).isTrue();
        assertThat(CalendarSchedule.isOpen(TODAY, TODAY.plusDays(3), TODAY)).isTrue();
        assertThat(CalendarSchedule.isOpen(start, TODAY.minusDays(1), TODAY)).isFalse();
        assertThat(CalendarSchedule.isOpen(TODAY.plusDays(1), TODAY.plusDays(5), TODAY)).isFalse();
    }
}
