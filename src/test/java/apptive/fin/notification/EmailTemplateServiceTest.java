package apptive.fin.notification;

import apptive.fin.notification.service.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    @Mock
    private NotificationProperties properties;

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService(properties);
    }

    @Test
    void 인증_이메일_템플릿_생성() {
        // given
        String verifyUrl = "https://y-fin.com/verify?token=abc123";

        // when
        String html = emailTemplateService.buildVerificationEmail(verifyUrl);

        // then
        assertThat(html).contains("이메일 인증");
        assertThat(html).contains(verifyUrl);
        assertThat(html).contains("Y-Fin");
    }

    @Test
    void 마감_알림_템플릿_생성() {
        // given
        String productName = "청년희망적금";
        String dDay = "D-3";
        String detailUrl = "https://y-fin.com/products/1";
        String unsubscribeUrl = "https://y-fin.com/unsubscribe";

        // when
        String html = emailTemplateService.buildDeadlineEmail(productName, dDay, detailUrl, unsubscribeUrl);

        // then
        assertThat(html).contains("마감");
        assertThat(html).contains(productName);
        assertThat(html).contains(dDay);
        assertThat(html).contains(detailUrl);
        assertThat(html).contains("수신거부");
    }

    @Test
    void 시작_알림_템플릿_생성() {
        // given
        String productName = "청년도약계좌";
        String detailUrl = "https://y-fin.com/products/2";
        String unsubscribeUrl = "https://y-fin.com/unsubscribe";

        // when
        String html = emailTemplateService.buildStartEmail(productName, detailUrl, unsubscribeUrl);

        // then
        assertThat(html).contains("시작");
        assertThat(html).contains(productName);
        assertThat(html).contains(detailUrl);
    }

    @Test
    void 만기_알림_템플릿_생성() {
        // given
        String productName = "정기적금";
        String dDay = "D-30";
        String unsubscribeUrl = "https://y-fin.com/unsubscribe";

        // when
        String html = emailTemplateService.buildMaturityEmail(productName, dDay, unsubscribeUrl);

        // then
        assertThat(html).contains("만기");
        assertThat(html).contains(productName);
        assertThat(html).contains(dDay);
    }

    @Test
    void 정정_알림_템플릿_생성() {
        // given
        String productName = "청년희망적금";
        String changeDetails = "금리 변경: 5.0% → 5.5%";
        String detailUrl = "https://y-fin.com/products/1";
        String unsubscribeUrl = "https://y-fin.com/unsubscribe";

        // when
        String html = emailTemplateService.buildCorrectionEmail(productName, changeDetails, detailUrl, unsubscribeUrl);

        // then
        assertThat(html).contains("변경");
        assertThat(html).contains(productName);
        assertThat(html).contains(changeDetails);
    }

    @Test
    void 다이제스트_템플릿_생성() {
        // given
        String digestContent = "<ul><li>상품1 마감 D-3</li><li>상품2 시작</li></ul>";
        String unsubscribeUrl = "https://y-fin.com/unsubscribe";

        // when
        String html = emailTemplateService.buildDigestEmail(digestContent, unsubscribeUrl);

        // then
        assertThat(html).contains("요약");
        assertThat(html).contains(digestContent);
    }
}
