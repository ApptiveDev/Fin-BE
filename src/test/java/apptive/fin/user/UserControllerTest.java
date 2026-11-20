package apptive.fin.user;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.auth.util.RefreshTokenCookieProvider;
import apptive.fin.global.error.GlobalExceptionHandler;
import apptive.fin.user.controller.UserController;
import apptive.fin.user.dto.UserUpdateRequestDto;
import apptive.fin.user.service.UserProfileService;
import apptive.fin.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final MockMvc mockMvc = standaloneSetup(new UserController(
                    userService, mock(UserProfileService.class), mock(RefreshTokenCookieProvider.class)))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @BeforeEach
    void 로그인한_사용자() {
        AuthUserDetails principal = new AuthUserDetails(1L, UserRole.RECOMMENDATION);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void 인증_정리() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 이메일_형식이_아니면_내_정보를_수정하지_않는다() throws Exception {
        mockMvc.perform(patch("/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "not-an-email", "name": "김핀" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C003"))
                .andExpect(jsonPath("$.message").value("올바른 이메일 형식이 아닙니다."));

        verifyNoInteractions(userService);
    }

    @Test
    void 이메일만_보내면_이름은_검사하지_않고_수정한다() throws Exception {
        mockMvc.perform(patch("/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "new@example.com" }
                                """))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(1L), any(UserUpdateRequestDto.class));
    }

    @Test
    void 이름이_공백이면_내_정보를_수정하지_않는다() throws Exception {
        mockMvc.perform(patch("/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "   " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이름은 공백일 수 없습니다."));

        verifyNoInteractions(userService);
    }
}
