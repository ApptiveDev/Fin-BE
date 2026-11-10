package apptive.fin.notification.controller;

import apptive.fin.notification.adapter.EmailSendPort;
import apptive.fin.notification.dto.EmailSendRequest;
import apptive.fin.notification.dto.EmailSendResult;
import apptive.fin.notification.entity.MailType;
import apptive.fin.notification.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Test Email", description = "테스트 이메일 발송 API (dev 전용)")
@Profile("dev")
@RestController
@RequestMapping("/test/email")
@RequiredArgsConstructor
public class TestEmailController {

    private final EmailSendPort emailSendPort;
    private final EmailTemplateService templateService;

    @Operation(summary = "테스트 이메일 발송", description = "테스트용 이메일을 발송합니다.")
    @PostMapping("/send")
    public ResponseEntity<TestEmailResponse> send(@RequestBody TestEmailRequest request) {
        String htmlBody = switch (request.templateType()) {
            case "verification" -> templateService.buildVerificationEmail("https://y-fin.com/verify?token=test");
            case "deadline" -> templateService.buildDeadlineEmail(
                    "테스트 상품", "D-3", "https://y-fin.com/products/1", "https://y-fin.com/unsubscribe"
            );
            case "start" -> templateService.buildStartEmail(
                    "테스트 상품", "https://y-fin.com/products/1", "https://y-fin.com/unsubscribe"
            );
            case "maturity" -> templateService.buildMaturityEmail(
                    "테스트 상품", "D-30", "https://y-fin.com/unsubscribe"
            );
            default -> "<h1>테스트 이메일</h1><p>Y-Fin. 테스트 이메일입니다.</p>";
        };

        EmailSendRequest sendRequest = EmailSendRequest.of(
                request.to(),
                "[Y-Fin. 테스트] " + request.templateType(),
                htmlBody,
                MailType.VERIFICATION
        );

        EmailSendResult result = emailSendPort.send(sendRequest);

        return ResponseEntity.ok(new TestEmailResponse(
                result.success(),
                result.messageId(),
                result.errorMessage()
        ));
    }

    public record TestEmailRequest(String to, String templateType) {}
    public record TestEmailResponse(boolean success, String messageId, String errorMessage) {}
}
