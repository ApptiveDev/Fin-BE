package apptive.fin.notification;

import apptive.fin.notification.dto.DeadlineEmailData;
import apptive.fin.notification.dto.DigestEmailData;
import apptive.fin.notification.dto.MaturityEmailData;
import apptive.fin.notification.service.EmailTemplateService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceTest {

    private static EmailTemplateService emailTemplateService;

    @BeforeAll
    static void setUp() {
        NotificationProperties properties = new NotificationProperties(
                new NotificationProperties.Resend("test-api-key", "test-webhook-secret"),
                new NotificationProperties.Mail("Y-Fin <noreply@y-fin.com>"),
                new NotificationProperties.Email("https://fin-qa.yuyeol3.uk/email/logo.png"),
                new NotificationProperties.Company("부산대학교", "noreply@y-fin.com"),
                "http://localhost:8080",
                "https://fin-qa.yuyeol3.uk",
                "test-secret"
        );

        emailTemplateService = new EmailTemplateService(properties);
    }

    @Nested
    @DisplayName("V2 템플릿 - Mustache 기반")
    class V2Templates {

        @Test
        @DisplayName("마감 알림 단건 템플릿 생성 - 긴급(D-3 이하)")
        void 마감_알림_긴급_템플릿_생성() {
            DeadlineEmailData data = DeadlineEmailData.builder()
                    .dday(3)
                    .urgent(true)
                    .typeLabel("금융 지원 정책")
                    .title("부산 청년 신용회복 지원금")
                    .startDateLabel("10월 1일")
                    .endDateLabel("10월 21일(수)")
                    .agency("부산광역시")
                    .applyMethod("온라인")
                    .ctaLabel("온통청년 보기")
                    .ctaUrl("https://ontong.example.com/policy/123")
                    .ctaExternal(true)
                    .allSchedulesUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings/notifications")
                    .unsubscribeUrl("https://y-fin.com/unsubscribe?token=abc")
                    .nextAlertDate("10.18")
                    .nextAlertDday(1)
                    .footerNotice(null)
                    .build();

            String html = emailTemplateService.buildDeadlineEmailV2(data);

            assertThat(html).contains("부산 청년 신용회복 지원금");
            assertThat(html).contains("3일");
            assertThat(html).contains("온통청년 보기");
            assertThat(html).contains("다음 알림");
        }

        @Test
        @DisplayName("마감 알림 단건 템플릿 생성 - 여유(D-7)")
        void 마감_알림_여유_템플릿_생성() {
            DeadlineEmailData data = DeadlineEmailData.builder()
                    .dday(7)
                    .urgent(false)
                    .typeLabel("청년 정책")
                    .title("청년희망적금")
                    .startDateLabel("9월 15일")
                    .endDateLabel("10월 30일(목)")
                    .agency("금융위원회")
                    .applyMethod("은행 방문")
                    .ctaLabel("상세 보기")
                    .ctaUrl("https://y-fin.com/products/1")
                    .ctaExternal(false)
                    .allSchedulesUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings/notifications")
                    .unsubscribeUrl("https://y-fin.com/unsubscribe?token=abc")
                    .nextAlertDate(null)
                    .nextAlertDday(null)
                    .footerNotice(null)
                    .build();

            String html = emailTemplateService.buildDeadlineEmailV2(data);

            assertThat(html).contains("7일");
            assertThat(html).contains("청년희망적금");
        }

        @Test
        @DisplayName("다이제스트 템플릿 생성")
        void 다이제스트_템플릿_생성() {
            DigestEmailData data = DigestEmailData.builder()
                    .count(3)
                    .baseDateLabel("10월 15일(화)")
                    .items(List.of(
                            DigestEmailData.DigestItem.builder()
                                    .badgeText("마감 D-1")
                                    .urgent(true)
                                    .title("부산 청년 신용회복 지원금")
                                    .subText("10.15 (목) 마감")
                                    .linkLabel("온통청년 보기")
                                    .linkUrl("https://ontong.example.com/1")
                                    .build(),
                            DigestEmailData.DigestItem.builder()
                                    .badgeText("마감 D-7")
                                    .urgent(false)
                                    .title("청년희망적금")
                                    .subText("10.21 (수) 마감")
                                    .linkLabel("상세 보기")
                                    .linkUrl("https://y-fin.com/products/2")
                                    .build()
                    ))
                    .moreCount(0)
                    .allUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings/notifications")
                    .unsubscribeUrl("https://y-fin.com/unsubscribe?token=abc")
                    .footerNotice(null)
                    .build();

            String html = emailTemplateService.buildDigestEmailV2(data);

            assertThat(html).contains("오늘 확인할 일정");
            assertThat(html).contains("3건");
            assertThat(html).contains("부산 청년 신용회복 지원금");
            assertThat(html).contains("청년희망적금");
            assertThat(html).contains("Y-Fin.에서 모두 보기");
        }

        @Test
        @DisplayName("만기 알림 템플릿 생성")
        void 만기_알림_템플릿_생성() {
            MaturityEmailData data = MaturityEmailData.builder()
                    .dday(30)
                    .productName("KB Star 정기예금")
                    .joinDate("2023년 11월 15일")
                    .termLabel("12개월")
                    .maturityDateLabel("2024년 11월 15일(금)")
                    .compareUrl("https://y-fin.com/compare?prefill=true")
                    .allSchedulesUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings/notifications")
                    .unsubscribeUrl("https://y-fin.com/unsubscribe?token=abc")
                    .footerNotice(null)
                    .build();

            String html = emailTemplateService.buildMaturityEmailV2(data);

            assertThat(html).contains("KB Star 정기예금");
            assertThat(html).contains("만기가");
            assertThat(html).contains("30일");
            assertThat(html).contains("내가 등록한 가입 상품");
            assertThat(html).contains("단기 예치");
            assertThat(html).contains("목돈 만들기");
            assertThat(html).contains("만기 후 선택지 비교하기");
        }

        @Test
        @DisplayName("인증 이메일 템플릿 생성")
        void 인증_이메일_템플릿_생성() {
            String html = emailTemplateService.buildVerificationEmail("https://y-fin.com/verify?token=abc123");

            assertThat(html).contains("이메일 인증");
            assertThat(html).contains("y-fin.com/verify");
            assertThat(html).contains("Y-Fin");
            assertThat(html).contains("24시간");
        }

        @Test
        @DisplayName("이메일 제목 생성")
        void 이메일_제목_생성() {
            // 마감 알림: D-7, D-3
            assertThat(emailTemplateService.buildDeadlineSubject("청년희망적금", 7))
                    .isEqualTo("[Y-Fin.] 청년희망적금 신청 마감까지 7일 남았어요");

            // 마감 알림: D-1 (내일 마감)
            assertThat(emailTemplateService.buildDeadlineSubject("청년희망적금", 1))
                    .isEqualTo("[Y-Fin.] 내일 청년희망적금 신청이 마감돼요");

            // 시작 알림: 당일
            assertThat(emailTemplateService.buildStartSubject("청년도약계좌", true))
                    .isEqualTo("[Y-Fin.] 청년도약계좌 신청이 오늘 시작됐어요");

            // 시작 알림: 전날
            assertThat(emailTemplateService.buildStartSubject("청년도약계좌", false))
                    .isEqualTo("[Y-Fin.] 내일 청년도약계좌 신청이 시작돼요");

            assertThat(emailTemplateService.buildDigestSubject(3))
                    .isEqualTo("[Y-Fin.] 오늘 확인할 일정 3건");

            // 만기 알림: 30일
            assertThat(emailTemplateService.buildMaturitySubject("KB정기예금", 30))
                    .isEqualTo("[Y-Fin.] KB정기예금 만기가 30일 남았어요");

            // 만기 알림: 7일 (일주일)
            assertThat(emailTemplateService.buildMaturitySubject("KB정기예금", 7))
                    .isEqualTo("[Y-Fin.] KB정기예금 만기가 일주일 남았어요");

            assertThat(emailTemplateService.buildVerificationSubject())
                    .isEqualTo("[Y-Fin.] 알림 받을 이메일을 확인해 주세요");
        }

        @Test
        @DisplayName("로고 URL과 회사 정보 포함")
        void 로고_URL과_회사_정보_포함() {
            DeadlineEmailData data = DeadlineEmailData.builder()
                    .dday(7)
                    .urgent(false)
                    .typeLabel("정책")
                    .title("테스트")
                    .startDateLabel("1일")
                    .endDateLabel("30일")
                    .agency("기관")
                    .applyMethod("방법")
                    .ctaLabel("보기")
                    .ctaUrl("https://example.com")
                    .ctaExternal(false)
                    .allSchedulesUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings")
                    .unsubscribeUrl("https://y-fin.com/unsub")
                    .build();

            String html = emailTemplateService.buildDeadlineEmailV2(data);

            assertThat(html).contains("https://fin-qa.yuyeol3.uk/email/logo.png");
            assertThat(html).contains("부산대학교");
            assertThat(html).contains("noreply@y-fin.com");
        }
    }

}
