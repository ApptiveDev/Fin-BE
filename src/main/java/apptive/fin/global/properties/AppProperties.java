package apptive.fin.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Frontend frontend,
    Cookie cookie,
    OAuth2 oAuth2
) {
    public record Cookie(
            boolean secure,
            String sameSite
    ) {}

    public record OAuth2(
            String successRedirectUrl,
            String failureRedirectUrl
    ) {}

    // 첫 주소가 기본값이다. 로그인을 시작한 출처를 알 수 없을 때 그 주소로 돌려보낸다.
    public record Frontend(
            List<String> urls
    ) {}
}
