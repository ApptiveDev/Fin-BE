package apptive.fin.global.error;

import apptive.fin.auth.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException은_에러_코드를_이용해_응답_반환() {
        BusinessException exception = new BusinessException(AuthErrorCode.UNAUTHORIZED);

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleBusinessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorResponseDto(
                AuthErrorCode.UNAUTHORIZED.getCode(),
                AuthErrorCode.UNAUTHORIZED.getMessage()
        ));
    }

    @Test
    void handleHttpRequestMethodNotSupportedException은_not_found_반환() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("POST");

        ResponseEntity<ErrorResponseDto> response =
                globalExceptionHandler.handleHttpRequestMethodNotSupportedException(exception);

        assertNotFoundResponse(response);
    }

    @Test
    void handleNoResourceFoundException은_not_found_반환() {
        NoResourceFoundException exception =
                new NoResourceFoundException(HttpMethod.GET, "/missing", "No static resource");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleNoResourceFoundException(exception);

        assertNotFoundResponse(response);
    }

    @Test
    void handleHttpMediaTypeNotAcceptableException은_내용_없음() {
        HttpMediaTypeNotAcceptableException exception =
                new HttpMediaTypeNotAcceptableException("Not acceptable");

        ResponseEntity<String> response =
                globalExceptionHandler.handleHttpMediaTypeNotAcceptableException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void handleException이_internal_server_error_반환() {
        Exception exception = new Exception("unexpected");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new ErrorResponseDto(
                CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        ));
    }

    private void assertNotFoundResponse(ResponseEntity<ErrorResponseDto> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorResponseDto(
                CommonErrorCode.NOT_FOUND.getCode(),
                CommonErrorCode.NOT_FOUND.getMessage()
        ));
    }
}
