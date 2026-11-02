package apptive.fin.auth.oauth;

import apptive.fin.global.properties.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 허용된 프론트 주소(app.frontend.urls) 중 로그인을 시작한 곳으로 OAuth 결과를 돌려보낸다.
 * 프론트는 window.location 이동으로 로그인을 시작하므로 Referer에 프론트 출처가 실린다.
 * OAuth 인가 요청이 이미 세션에 저장되므로 출처도 같은 세션에 둔다.
 */
@Component
@RequiredArgsConstructor
public class FrontendOrigins {

    private static final String LOGIN_ORIGIN = FrontendOrigins.class.getName() + ".LOGIN_ORIGIN";

    private final AppProperties appProperties;

    public void rememberLoginOrigin(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.removeAttribute(LOGIN_ORIGIN);

        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null) {
            return;
        }
        String origin = originOf(referer);
        if (appProperties.frontend().urls().contains(origin)) {
            session.setAttribute(LOGIN_ORIGIN, origin);
        }
    }

    /** 로그인을 시작한 허용 출처. 알 수 없으면 첫 주소. */
    public String redirectBase(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object origin = session == null ? null : session.getAttribute(LOGIN_ORIGIN);
        return origin != null ? (String) origin : appProperties.frontend().urls().getFirst();
    }

    private static String originOf(String referer) {
        UriComponents uri = UriComponentsBuilder.fromUriString(referer).build();
        return UriComponentsBuilder.newInstance()
                .scheme(uri.getScheme()).host(uri.getHost()).port(uri.getPort())
                .toUriString();
    }
}
