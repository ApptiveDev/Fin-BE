package apptive.fin.term;

import apptive.fin.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TermErrorCode implements ErrorCode {

    TERM_NOT_FOUND(HttpStatus.NOT_FOUND,"001","존재하지 않는 약관."),
    REQUIRED_TERM_NOT_AGREED(HttpStatus.BAD_REQUEST,"002","필수 약관 미동의."),
    ALREADY_AGREED(HttpStatus.CONFLICT,"003","이미 동의한 약관."),
    TERM_ID_MISMATCH(HttpStatus.BAD_REQUEST,"004","요청한 약관 ID가 올바르지 않음.");

    private final String codePrefix = "T";
    private final HttpStatus httpStatus;
    private final String errNum;
    private final String message;
}
