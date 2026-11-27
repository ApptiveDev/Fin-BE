package apptive.fin.notification;

import apptive.fin.global.error.BusinessException;
import apptive.fin.global.error.GlobalExceptionHandler;
import apptive.fin.notification.controller.EmailVerificationController;
import apptive.fin.notification.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class EmailVerificationControllerTest {

    private EmailVerificationService emailVerificationService;
    private NotificationProperties notificationProperties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        emailVerificationService = mock(EmailVerificationService.class);
        notificationProperties = mock(NotificationProperties.class);
        given(notificationProperties.frontendBaseUrl()).willReturn("https://y-fin.com");

        mockMvc = standaloneSetup(new EmailVerificationController(emailVerificationService, notificationProperties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    void 이메일_인증_성공시_프론트엔드로_리다이렉트() throws Exception {
        // given
        String token = "validToken123";
        willDoNothing().given(emailVerificationService).verifyEmail(token);

        // when & then
        mockMvc.perform(get("/notifications/email/verify")
                        .param("token", token)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://y-fin.com/verify-success"));

        verify(emailVerificationService).verifyEmail(token);
    }

    @Test
    void 이메일_인증_잘못된_토큰() throws Exception {
        // given
        String invalidToken = "invalidToken";
        willThrow(new BusinessException(NotificationErrorCode.INVALID_TOKEN))
                .given(emailVerificationService).verifyEmail(invalidToken);

        // when & then
        mockMvc.perform(get("/notifications/email/verify")
                        .param("token", invalidToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void 이메일_인증_토큰_파라미터_누락() throws Exception {
        // when & then
        mockMvc.perform(get("/notifications/email/verify"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 이메일_인증_만료된_토큰() throws Exception {
        // given
        String expiredToken = "expiredToken";
        willThrow(new BusinessException(NotificationErrorCode.INVALID_TOKEN))
                .given(emailVerificationService).verifyEmail(expiredToken);

        // when & then
        mockMvc.perform(get("/notifications/email/verify")
                        .param("token", expiredToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("N002"));
    }
}
