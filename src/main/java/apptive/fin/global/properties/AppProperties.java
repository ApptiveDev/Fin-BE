package apptive.fin.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cookie")
public record RefreshTokenCookieProperties(
        boolean secure,
        String sameSite
) {
}
