package apptive.fin.notification;

import apptive.fin.global.error.BusinessException;
import apptive.fin.notification.adapter.EmailSendPort;
import apptive.fin.notification.dto.EmailSendResult;
import apptive.fin.notification.entity.EmailVerification;
import apptive.fin.notification.repository.EmailSendLogRepository;
import apptive.fin.notification.repository.EmailVerificationRepository;
import apptive.fin.notification.service.EmailTemplateService;
import apptive.fin.notification.service.EmailVerificationService;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailSendLogRepository emailSendLogRepository;
    @Mock
    private EmailSendPort emailSendPort;
    @Mock
    private EmailTemplateService emailTemplateService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationProperties properties;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("테스트유저")
                .email("test@example.com")
                .provider("google")
                .providerId("12345")
                .build();
    }

    @Test
    void 인증_이메일_발송_성공() {
        // given
        Long userId = 1L;
        String email = "test@example.com";

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(emailVerificationRepository.findLatestByUserIdAndEmail(userId, email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("hashedToken");
        given(properties.appBaseUrl()).willReturn("https://y-fin.com");
        given(emailTemplateService.buildVerificationEmail(anyString())).willReturn("<html>인증</html>");
        given(emailSendPort.send(any())).willReturn(EmailSendResult.success("msg_123"));
        given(emailSendLogRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(emailVerificationRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        emailVerificationService.sendVerificationEmail(userId, email);

        // then
        verify(emailVerificationRepository).save(any(EmailVerification.class));
        verify(emailSendPort).send(any());
        verify(emailSendLogRepository).save(any());
    }

    @Test
    void 존재하지_않는_유저_예외발생() {
        // given
        Long userId = 999L;
        String email = "test@example.com";

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(userId, email))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 재발송_쿨다운_중_예외발생() {
        // given
        Long userId = 1L;
        String email = "test@example.com";

        EmailVerification recentVerification = mock(EmailVerification.class);
        given(recentVerification.canResend()).willReturn(false);

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(emailVerificationRepository.findLatestByUserIdAndEmail(userId, email))
                .willReturn(Optional.of(recentVerification));

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(userId, email))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 이메일_발송_실패시_예외발생() {
        // given
        Long userId = 1L;
        String email = "test@example.com";

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(emailVerificationRepository.findLatestByUserIdAndEmail(userId, email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("hashedToken");
        given(properties.appBaseUrl()).willReturn("https://y-fin.com");
        given(emailTemplateService.buildVerificationEmail(anyString())).willReturn("<html>인증</html>");
        given(emailSendPort.send(any())).willReturn(EmailSendResult.failure("발송 실패"));
        given(emailSendLogRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(emailVerificationRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(userId, email))
                .isInstanceOf(BusinessException.class);
    }
}
