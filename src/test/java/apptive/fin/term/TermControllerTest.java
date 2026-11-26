package apptive.fin.term;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.global.error.GlobalExceptionHandler;
import apptive.fin.term.controller.TermController;
import apptive.fin.term.service.TermService;
import apptive.fin.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TermControllerTest {

    private final TermService termService = mock(TermService.class);
    private final MockMvc mockMvc = standaloneSetup(new TermController(termService))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @BeforeEach
    void 로그인한_사용자() {
        AuthUserDetails principal = new AuthUserDetails(1L, UserRole.BEFORE_AGREED);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void 인증_정리() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void agreements가_없으면_잘못된_입력으로_응답한다() throws Exception {
        mockMvc.perform(post("/term")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C003"));

        verifyNoInteractions(termService);
    }
}
