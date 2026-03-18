package apptive.fin.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Cookie cookie,
    OAuth2 oAuth2
) {
    public record Cookie(
            boolean secure,
            String sameSite
    ) {}

    public record OAuth2(
            String successRedirectUrl
    ) {}
}
