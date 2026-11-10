package apptive.fin.notification.service;

import apptive.fin.global.error.BusinessException;
import apptive.fin.notification.NotificationErrorCode;
import apptive.fin.notification.NotificationProperties;
import apptive.fin.notification.adapter.EmailSendPort;
import apptive.fin.notification.dto.EmailSendRequest;
import apptive.fin.notification.dto.EmailSendResult;
import apptive.fin.notification.entity.EmailSendLog;
import apptive.fin.notification.entity.EmailSendStatus;
import apptive.fin.notification.entity.EmailVerification;
import apptive.fin.notification.entity.MailType;
import apptive.fin.notification.repository.EmailSendLogRepository;
import apptive.fin.notification.repository.EmailVerificationRepository;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int TOKEN_EXPIRY_HOURS = 24;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailSendLogRepository emailSendLogRepository;
    private final EmailSendPort emailSendPort;
    private final EmailTemplateService emailTemplateService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationProperties properties;

    @Transactional
    public void sendVerificationEmail(Long userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.USER_NOT_FOUND));

        emailVerificationRepository.findLatestByUserIdAndEmail(userId, email)
                .ifPresent(existing -> {
                    if (!existing.canResend()) {
                        throw new BusinessException(NotificationErrorCode.RESEND_COOLDOWN);
                    }
                });

        String rawToken = generateSecureToken();
        String tokenHash = passwordEncoder.encode(rawToken);

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .email(email)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                .build();

        emailVerificationRepository.save(verification);

        String verifyUrl = properties.appBaseUrl() + "/notifications/email/verify?token=" + rawToken;
        String htmlBody = emailTemplateService.buildVerificationEmail(verifyUrl);

        EmailSendRequest request = EmailSendRequest.of(
                email,
                "[Y-Fin.] 이메일 인증을 완료해주세요",
                htmlBody,
                MailType.VERIFICATION
        );

        EmailSendResult result = emailSendPort.send(request);

        emailSendLogRepository.save(EmailSendLog.builder()
                .toEmail(email)
                .mailType(MailType.VERIFICATION)
                .resendMessageId(result.messageId())
                .status(result.success() ? EmailSendStatus.SENT : EmailSendStatus.FAILED)
                .errorMessage(result.errorMessage())
                .build());

        if (!result.success()) {
            throw new BusinessException(NotificationErrorCode.EMAIL_SEND_FAILED);
        }

        log.info("Verification email sent to {} for user {}", email, userId);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerification verification = findValidVerification(rawToken);

        verification.markAsUsed();

        User user = verification.getUser();
        user.verifyEmail();
        user.updateEmail(verification.getEmail());

        log.info("Email verified for user {}: {}", user.getId(), verification.getEmail());
    }

    private EmailVerification findValidVerification(String rawToken) {
        return emailVerificationRepository.findAll().stream()
                .filter(v -> !v.isUsed() && !v.isExpired())
                .filter(v -> passwordEncoder.matches(rawToken, v.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.INVALID_TOKEN));
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
