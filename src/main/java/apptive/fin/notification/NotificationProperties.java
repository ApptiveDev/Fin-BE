package apptive.fin.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        Resend resend,
        Mail mail,
        String appBaseUrl,
        String unsubscribeSigningSecret
) {
    public record Resend(String apiKey, String webhookSecret) {}
    public record Mail(String from) {}
}
