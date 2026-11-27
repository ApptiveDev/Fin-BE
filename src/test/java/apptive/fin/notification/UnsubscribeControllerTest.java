package apptive.fin.notification;

import apptive.fin.global.error.BusinessException;
import apptive.fin.global.error.GlobalExceptionHandler;
import apptive.fin.notification.controller.UnsubscribeController;
import apptive.fin.notification.service.UnsubscribeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UnsubscribeControllerTest {

    private UnsubscribeService unsubscribeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        unsubscribeService = mock(UnsubscribeService.class);
        mockMvc = standaloneSetup(new UnsubscribeController(unsubscribeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    void 수신거부_성공() throws Exception {
        // given
        String email = "test@example.com";
        String signature = "validSignature";
        willDoNothing().given(unsubscribeService).unsubscribe(email, signature);

        // when & then
        mockMvc.perform(get("/notifications/unsubscribe")
                        .param("email", email)
                        .param("sig", signature)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk());

        verify(unsubscribeService).unsubscribe(email, signature);
    }

    @Test
    void 수신거부_잘못된_서명() throws Exception {
        // given
        String email = "test@example.com";
        String invalidSignature = "invalidSignature";
        willThrow(new BusinessException(NotificationErrorCode.INVALID_UNSUBSCRIBE_SIGNATURE))
                .given(unsubscribeService).unsubscribe(email, invalidSignature);

        // when & then
        mockMvc.perform(get("/notifications/unsubscribe")
                        .param("email", email)
                        .param("sig", invalidSignature))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("N006"));
    }

    @Test
    void 수신거부_이메일_파라미터_누락() throws Exception {
        // when & then
        mockMvc.perform(get("/notifications/unsubscribe")
                        .param("sig", "someSignature"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 수신거부_서명_파라미터_누락() throws Exception {
        // when & then
        mockMvc.perform(get("/notifications/unsubscribe")
                        .param("email", "test@example.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 수신거부_해제_성공() throws Exception {
        // given
        String email = "test@example.com";
        String signature = "validSignature";
        willDoNothing().given(unsubscribeService).resubscribe(email, signature);

        // when & then
        mockMvc.perform(post("/notifications/unsubscribe/cancel")
                        .param("email", email)
                        .param("sig", signature)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk());

        verify(unsubscribeService).resubscribe(email, signature);
    }

    @Test
    void 수신거부_해제_이메일_파라미터_누락() throws Exception {
        // when & then
        mockMvc.perform(post("/notifications/unsubscribe/cancel")
                        .param("sig", "someSignature"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 수신거부_해제_서명_파라미터_누락() throws Exception {
        // when & then
        mockMvc.perform(post("/notifications/unsubscribe/cancel")
                        .param("email", "test@example.com"))
                .andExpect(status().isBadRequest());
    }
}
