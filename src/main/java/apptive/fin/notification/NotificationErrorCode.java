package apptive.fin.notification;

import apptive.fin.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "001", "사용자를 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "002", "유효하지 않거나 만료된 인증 토큰입니다."),
    RESEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "003", "1분 후에 다시 시도해주세요."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "004", "이메일 발송에 실패했습니다."),
    ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "005", "이미 인증된 이메일입니다."),
    INVALID_UNSUBSCRIBE_SIGNATURE(HttpStatus.BAD_REQUEST, "006", "잘못된 수신거부 요청입니다."),
    EMAIL_UNSUBSCRIBED(HttpStatus.BAD_REQUEST, "007", "수신거부된 이메일입니다."),
    SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "008", "알림 설정을 찾을 수 없습니다."),
    SETTING_ALREADY_EXISTS(HttpStatus.CONFLICT, "009", "이미 등록된 알림 설정입니다."),
    INVALID_DEADLINE_DATE(HttpStatus.BAD_REQUEST, "010", "마감일은 오늘 이후여야 합니다."),
    TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "011", "대상을 찾을 수 없습니다."),
    ;

    private final String codePrefix = "N";
    private final HttpStatus httpStatus;
    private final String errNum;
    private final String message;
}
