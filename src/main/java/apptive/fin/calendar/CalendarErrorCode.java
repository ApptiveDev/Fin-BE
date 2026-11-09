package apptive.fin.calendar;

import apptive.fin.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CalendarErrorCode implements ErrorCode {

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "001", "조회 시작일(from)은 종료일(to)보다 늦을 수 없습니다."),
    ;

    private final String codePrefix = "CA";
    private final HttpStatus httpStatus;
    private final String errNum;
    private final String message;
}
