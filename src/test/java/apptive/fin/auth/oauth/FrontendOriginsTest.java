package apptive.fin.auth.oauth;

import apptive.fin.global.properties.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendOriginsTest {

    private final FrontendOrigins frontendOrigins = new FrontendOrigins(new AppProperties(
            new AppProperties.Frontend(List.of("http://localhost:5173", "https://fe.example.com")),
            null,
            null
    ));

    @Test
    void 허용된_출처에서_시작한_로그인은_그_출처로_돌아간다() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest loginStart = requestWithReferer(session, "https://fe.example.com/login");

        frontendOrigins.rememberLoginOrigin(loginStart);

        assertThat(frontendOrigins.redirectBase(callback(session))).isEqualTo("https://fe.example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://fe.example.com",
            "https://fe.example.com/",
            "https://fe.example.com/login?next=%2Fmypage",
            "https://fe.example.com/login#top",
    })
    void 허용된_출처의_어느_페이지에서_시작해도_그_출처로_돌아간다(String referer) {
        MockHttpSession session = new MockHttpSession();

        frontendOrigins.rememberLoginOrigin(requestWithReferer(session, referer));

        assertThat(frontendOrigins.redirectBase(callback(session))).isEqualTo("https://fe.example.com");
    }

    @Test
    void 허용되지_않은_출처에서_시작한_로그인은_기본_주소로_돌아간다() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest loginStart = requestWithReferer(session, "https://evil.example.com/login");

        frontendOrigins.rememberLoginOrigin(loginStart);

        assertThat(frontendOrigins.redirectBase(callback(session))).isEqualTo("http://localhost:5173");
    }

    @Test
    void Referer_없이_시작한_로그인은_기본_주소로_돌아간다() {
        MockHttpSession session = new MockHttpSession();

        frontendOrigins.rememberLoginOrigin(callback(session));

        assertThat(frontendOrigins.redirectBase(callback(session))).isEqualTo("http://localhost:5173");
    }

    @Test
    void 같은_세션에서_다시_로그인하면_이전_로그인의_출처를_쓰지_않는다() {
        MockHttpSession session = new MockHttpSession();
        frontendOrigins.rememberLoginOrigin(requestWithReferer(session, "https://fe.example.com/login"));

        frontendOrigins.rememberLoginOrigin(callback(session));

        assertThat(frontendOrigins.redirectBase(callback(session))).isEqualTo("http://localhost:5173");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "not a url",
            "https://fe.example.com.evil.com/login",   // 허용 출처를 접두사로 가진 다른 호스트
            "https://fe.example.com:8443/login",       // 포트가 다르면 다른 출처
            "http://fe.example.com/login",             // 스킴이 다르면 다른 출처
    })
    void 허용_출처와_정확히_같지_않은_Referer는_기본_주소로_돌아간다(String referer) {
        MockHttpSession session = new MockHttpSession();

        frontendOrigins.rememberLoginOrigin(requestWithReferer(session, referer));

        assertThat(frontendOrigins.redirectBase(callback(session))).isEqualTo("http://localhost:5173");
    }

    private static MockHttpServletRequest requestWithReferer(MockHttpSession session, String referer) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.addHeader(HttpHeaders.REFERER, referer);
        return request;
    }

    private static MockHttpServletRequest callback(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }
}
