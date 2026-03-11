package apptive.fin.auth;

import apptive.fin.global.dto.GenericDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("refresh_token") String refreshToken) {
        authService.logout(refreshToken);

        ResponseCookie cookie = refreshTokenCookieProvider.createLogoutCookie();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();

    }

    @PostMapping("/refresh")
    public ResponseEntity<GenericDataDto<String>> refresh(@CookieValue("refresh_token") String refreshToken) {
        LoginResponseDto res = authService.refresh(refreshToken);
        ResponseCookie cookie
                = refreshTokenCookieProvider.createRefreshTokenCookie(res.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new GenericDataDto<>(res.accessToken()));
    }
}
