package apptive.fin.notification.service;

import apptive.fin.global.error.BusinessException;
import apptive.fin.notification.NotificationErrorCode;
import apptive.fin.notification.NotificationProperties;
import apptive.fin.notification.entity.NotificationGlobal;
import apptive.fin.notification.entity.PausedReason;
import apptive.fin.notification.repository.NotificationGlobalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnsubscribeService {

    private final NotificationGlobalRepository notificationGlobalRepository;
    private final NotificationProperties properties;

    public String generateUnsubscribeUrl(String email) {
        String signature = generateSignature(email);
        return properties.appBaseUrl() + "/notifications/unsubscribe?email=" + email + "&sig=" + signature;
    }

    @Transactional
    public void unsubscribe(String email, String signature) {
        validateSignature(email, signature);

        NotificationGlobal global = notificationGlobalRepository.findByEmail(email)
                .orElseGet(() -> notificationGlobalRepository.save(NotificationGlobal.builder()
                        .email(email)
                        .build()));

        global.pause(PausedReason.UNSUBSCRIBE);
        log.info("Email unsubscribed: {}", email);
    }

    @Transactional
    public void resubscribe(String email) {
        notificationGlobalRepository.findByEmail(email)
                .ifPresent(NotificationGlobal::resume);
        log.info("Email resubscribed: {}", email);
    }

    public boolean isUnsubscribed(String email) {
        return notificationGlobalRepository.existsByEmailAndEnabledFalse(email);
    }

    private String generateSignature(String email) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    properties.unsubscribeSigningSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(email.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private void validateSignature(String email, String signature) {
        String expected = generateSignature(email);
        if (!expected.equals(signature)) {
            throw new BusinessException(NotificationErrorCode.INVALID_UNSUBSCRIBE_SIGNATURE);
        }
    }
}
