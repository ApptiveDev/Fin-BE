package apptive.fin.notification;

import apptive.fin.global.error.BusinessException;
import apptive.fin.notification.entity.NotificationGlobal;
import apptive.fin.notification.entity.PausedReason;
import apptive.fin.notification.repository.NotificationGlobalRepository;
import apptive.fin.notification.service.UnsubscribeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnsubscribeServiceTest {

    @Mock
    private NotificationGlobalRepository notificationGlobalRepository;

    @Mock
    private NotificationProperties properties;

    private UnsubscribeService unsubscribeService;

    private static final String TEST_SECRET = "test-secret-key-for-signing";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        lenient().when(properties.appBaseUrl()).thenReturn("https://y-fin.com");
        lenient().when(properties.unsubscribeSigningSecret()).thenReturn(TEST_SECRET);
        unsubscribeService = new UnsubscribeService(notificationGlobalRepository, properties);
    }

    @Test
    void 수신거부_URL_생성_성공() {
        // when
        String url = unsubscribeService.generateUnsubscribeUrl(TEST_EMAIL);

        // then
        assertThat(url).startsWith("https://y-fin.com/notifications/unsubscribe");
        assertThat(url).contains("email=" + TEST_EMAIL);
        assertThat(url).contains("sig=");
    }

    @Test
    void 수신거부_처리_성공_기존_레코드_있음() {
        // given
        String validSignature = extractSignature(unsubscribeService.generateUnsubscribeUrl(TEST_EMAIL));
        NotificationGlobal existing = NotificationGlobal.builder().email(TEST_EMAIL).build();

        given(notificationGlobalRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(existing));

        // when
        unsubscribeService.unsubscribe(TEST_EMAIL, validSignature);

        // then
        assertThat(existing.isEnabled()).isFalse();
        assertThat(existing.getPausedReason()).isEqualTo(PausedReason.UNSUBSCRIBE);
    }

    @Test
    void 수신거부_처리_성공_신규_레코드_생성() {
        // given
        String validSignature = extractSignature(unsubscribeService.generateUnsubscribeUrl(TEST_EMAIL));

        given(notificationGlobalRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());
        given(notificationGlobalRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        unsubscribeService.unsubscribe(TEST_EMAIL, validSignature);

        // then
        verify(notificationGlobalRepository).save(any(NotificationGlobal.class));
    }

    @Test
    void 잘못된_서명_예외발생() {
        // given
        String invalidSignature = "invalid-signature";

        // when & then
        assertThatThrownBy(() -> unsubscribeService.unsubscribe(TEST_EMAIL, invalidSignature))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 수신거부_해제_성공() {
        // given
        NotificationGlobal existing = NotificationGlobal.builder().email(TEST_EMAIL).build();
        existing.pause(PausedReason.UNSUBSCRIBE);

        given(notificationGlobalRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(existing));

        // when
        unsubscribeService.resubscribe(TEST_EMAIL);

        // then
        assertThat(existing.isEnabled()).isTrue();
        assertThat(existing.getPausedReason()).isNull();
    }

    @Test
    void 수신거부_상태_확인() {
        // given
        given(notificationGlobalRepository.existsByEmailAndEnabledFalse(TEST_EMAIL)).willReturn(true);

        // when
        boolean result = unsubscribeService.isUnsubscribed(TEST_EMAIL);

        // then
        assertThat(result).isTrue();
    }

    private String extractSignature(String url) {
        return url.substring(url.indexOf("sig=") + 4);
    }
}
