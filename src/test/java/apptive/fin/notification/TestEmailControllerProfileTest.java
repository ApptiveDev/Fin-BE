package apptive.fin.notification;

import apptive.fin.notification.adapter.EmailSendPort;
import apptive.fin.notification.controller.TestEmailController;
import apptive.fin.notification.service.EmailTemplateService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 테스트 발송 엔드포인트는 인증 없이 아무 주소로나 메일을 보내므로 로컬 개발(dev 단독)에서만 켠다.
 * 개발 서버는 dev,server, 운영은 server 프로필로 뜬다(deploy.yml).
 */
class TestEmailControllerProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(EmailSendPort.class, () -> mock(EmailSendPort.class))
            .withBean(EmailTemplateService.class, () -> mock(EmailTemplateService.class))
            .withUserConfiguration(TestEmailController.class);

    @ParameterizedTest(name = "[{0}] 활성={1}")
    @CsvSource({
            "dev, true",        // 로컬 bootRun
            "'dev,test', true", // 통합 테스트
            "'dev,server', false", // 개발 서버
            "server, false"     // 운영 서버
    })
    void 로컬_개발_프로필에서만_켜진다(String profiles, boolean expected) {
        runner.withPropertyValues("spring.profiles.active=" + profiles)
                .run(context -> assertThat(context.getBeanNamesForType(TestEmailController.class)).hasSize(expected ? 1 : 0));
    }
}
