package apptive.fin.notification.controller;

import apptive.fin.notification.dto.SendVerificationRequest;
import apptive.fin.notification.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Email Verification", description = "이메일 인증 API")
@RestController
@RequestMapping("/notifications/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "인증 이메일 발송", description = "사용자에게 인증 이메일을 발송합니다.")
    @PostMapping("/send-verification")
    public ResponseEntity<Void> sendVerification(
            @AuthenticationPrincipal Long userId,
            @RequestBody SendVerificationRequest request
    ) {
        emailVerificationService.sendVerificationEmail(userId, request.email());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이메일 인증", description = "토큰을 검증하여 이메일 인증을 완료합니다.")
    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }
}
