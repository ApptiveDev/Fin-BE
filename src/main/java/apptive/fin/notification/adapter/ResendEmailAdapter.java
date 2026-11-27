package apptive.fin.notification.adapter;

import apptive.fin.notification.NotificationProperties;
import apptive.fin.notification.dto.EmailSendRequest;
import apptive.fin.notification.dto.EmailSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class ResendEmailAdapter implements EmailSendPort {

    private final WebClient webClient;
    private final String mailFrom;

    public ResendEmailAdapter(NotificationProperties properties) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.resend().apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.mailFrom = properties.mail().from();
    }

    @Override
    public EmailSendResult send(EmailSendRequest request) {
        try {
            Map<String, Object> body = Map.of(
                    "from", mailFrom,
                    "to", request.to(),
                    "subject", request.subject(),
                    "html", request.htmlBody()
            );

            ResendResponse response = webClient.post()
                    .uri("/emails")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(ResendResponse.class)
                    .block();

            if (response != null && response.id() != null) {
                log.info("Email sent successfully to {} with messageId: {}", request.to(), response.id());
                return EmailSendResult.success(response.id());
            }

            return EmailSendResult.failure("No response from Resend API");
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", request.to(), e.getMessage());
            return EmailSendResult.failure(e.getMessage());
        }
    }

    private record ResendResponse(String id) {}
}
