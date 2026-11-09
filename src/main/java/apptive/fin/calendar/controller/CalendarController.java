package apptive.fin.calendar.controller;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.calendar.dto.CalendarEventsResponseDto;
import apptive.fin.calendar.dto.CalendarSummaryResponseDto;
import apptive.fin.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    /** from ~ to(양 끝 포함)에 찍히는 시작·마감 일정. 월간 그리드, 주간 스트립, 리스트 뷰가 같이 쓴다. */
    @GetMapping("/events")
    public CalendarEventsResponseDto getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthUserDetails userDetails
    ) {
        return calendarService.getEvents(from, to, userDetails);
    }

    /** 보고 있는 월과 무관한 오늘 기준 정보: 지금 신청 가능, 연중 접수, 상시 신청. */
    @GetMapping("/summary")
    public CalendarSummaryResponseDto getSummary(@AuthenticationPrincipal AuthUserDetails userDetails) {
        return calendarService.getSummary(userDetails);
    }
}
