package apptive.fin.auth.oauth;

import apptive.fin.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

// 프론트가 여러 곳(로컬 개발 서버, Vercel 배포본)에서 같은 dev 서버를 쓰는 경우.
@TestPropertySource(properties = "app.frontend.urls=http://localhost:5173,https://fe.example.com")
class FrontendOriginsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void 기본_주소가_아닌_허용_출처의_CORS_요청도_허용한다() throws Exception {
        mockMvc.perform(options("/api/categories")
                        .header(HttpHeaders.ORIGIN, "https://fe.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://fe.example.com"));
    }

    @Test
    void 로그인을_시작한_허용_출처로_OAuth_실패_결과를_돌려보낸다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult authorization = mockMvc.perform(get("/oauth2/authorization/google")
                        .session(session)
                        .header(HttpHeaders.REFERER, "https://fe.example.com/login"))
                .andReturn();
        String state = UriComponentsBuilder.fromUriString(authorization.getResponse().getRedirectedUrl())
                .build().getQueryParams().getFirst("state");

        mockMvc.perform(get("/login/oauth2/code/google")
                        .session(session)
                        .param("error", "access_denied")
                        .param("state", state))
                .andExpect(redirectedUrl("https://fe.example.com/login-error"));
    }

    @Test
    void Referer가_없으면_OAuth_실패_결과를_첫_주소로_돌려보낸다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult authorization = mockMvc.perform(get("/oauth2/authorization/google").session(session))
                .andReturn();
        String state = UriComponentsBuilder.fromUriString(authorization.getResponse().getRedirectedUrl())
                .build().getQueryParams().getFirst("state");

        mockMvc.perform(get("/login/oauth2/code/google")
                        .session(session)
                        .param("error", "access_denied")
                        .param("state", state))
                .andExpect(redirectedUrl("http://localhost:5173/login-error"));
    }
}
