package apptive.fin.auth;

import apptive.fin.global.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieProvider {

    private final boolean isSecure;
    private final String sameSite;
    private final JwtUtil jwtUtil;
    // 생성자 주입
    public RefreshTokenCookieProvider(
            JwtUtil jwtUtil,
            @Value("${app.cookie.secure:false}") boolean isSecure,
            @Value("${app.cookie.same-site:None}") String sameSite
    ) {
        this.isSecure = isSecure;
        this.sameSite = sameSite;
        this.jwtUtil = jwtUtil;
    }

    // 리프레시 토큰용 쿠키 생성 로직 캡슐화
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(jwtUtil.getRefreshExpiration())
                .sameSite(sameSite)
                .build();
    }

    // 로그아웃 시 쿠키 삭제용 (maxAge=0) 로직 등도 여기에 추가 가능
    public ResponseCookie createLogoutCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }
}
