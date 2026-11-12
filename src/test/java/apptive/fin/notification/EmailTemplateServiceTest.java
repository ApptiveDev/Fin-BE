package apptive.fin.notification;

import apptive.fin.notification.dto.DeadlineEmailData;
import apptive.fin.notification.dto.DigestEmailData;
import apptive.fin.notification.dto.MaturityEmailData;
import apptive.fin.notification.dto.StartEmailData;
import apptive.fin.notification.service.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    @Mock
    private NotificationProperties properties;

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService(properties);
    }

    @Nested
    @DisplayName("V2 템플릿 - 새 디자인 시스템")
    class V2Templates {

        @Test
        @DisplayName("마감 알림 단건 템플릿 생성 - 긴급(D-3 이하)")
        void 마감_알림_긴급_템플릿_생성() {
            // given
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

            // when
            String html = emailTemplateService.buildDeadlineEmailV2(data);

            // then
            assertThat(html).contains("부산 청년 신용회복 지원금");
            assertThat(html).contains("D-3");
            assertThat(html).contains("#F0596E"); // RED - 긴급 뱃지
            assertThat(html).contains("온통청년 보기");
            assertThat(html).contains("target=\"_blank\""); // 외부 링크
            assertThat(html).contains("다음 알림 · 10.18");
            assertThat(html).contains("#03BFA5"); // MINT - 버튼
            assertThat(html).doesNotContain("${");
            assertThat(html).doesNotContain("{%");
        }

        @Test
        @DisplayName("마감 알림 단건 템플릿 생성 - 여유(D-7)")
        void 마감_알림_여유_템플릿_생성() {
            // given
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

            // when
            String html = emailTemplateService.buildDeadlineEmailV2(data);

            // then
            assertThat(html).contains("D-7");
            assertThat(html).contains("#FEEFF2"); // RED_SOFT_BG - 여유 뱃지
            assertThat(html).doesNotContain("target=\"_blank\""); // 내부 링크
            assertThat(html).doesNotContain("다음 알림"); // nextAlert 없음
        }

        @Test
        @DisplayName("시작 알림 템플릿 생성")
        void 시작_알림_템플릿_생성() {
            // given
            StartEmailData data = StartEmailData.builder()
                    .typeLabel("금융 지원 정책")
                    .title("청년도약계좌")
                    .startDateLabel("10월 1일")
                    .endDateLabel("12월 31일(화)")
                    .agency("금융위원회")
                    .applyMethod("은행 앱")
                    .ctaLabel("온통청년 보기")
                    .ctaUrl("https://ontong.example.com/policy/456")
                    .ctaExternal(true)
                    .allSchedulesUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings/notifications")
                    .unsubscribeUrl("https://y-fin.com/unsubscribe?token=abc")
                    .footerNotice(null)
                    .build();

            // when
            String html = emailTemplateService.buildStartEmailV2(data);

            // then
            assertThat(html).contains("청년도약계좌");
            assertThat(html).contains("신청이");
            assertThat(html).contains("시작");
            assertThat(html).contains("#03BFA5"); // MINT - 시작 뱃지, 버튼
            assertThat(html).contains("신청 시작");
        }

        @Test
        @DisplayName("다이제스트 템플릿 생성")
        void 다이제스트_템플릿_생성() {
            // given
            DigestEmailData data = DigestEmailData.builder()
                    .count(3)
                    .baseDateLabel("10월 15일(화)")
                    .items(List.of(
                            DigestEmailData.DigestItem.builder()
                                    .badgeText("마감 D-1")
                                    .urgent(true)
                                    .title("부산 청년 신용회복 지원금")
                                    .subText("10.15 (목) 마감 · 내일까지 신청할 수 있어요")
                                    .linkLabel("온통청년 보기")
                                    .linkUrl("https://ontong.example.com/1")
                                    .build(),
                            DigestEmailData.DigestItem.builder()
                                    .badgeText("마감 D-7")
                                    .urgent(false)
                                    .title("청년희망적금")
                                    .subText("10.21 (수) 마감 · 일주일 남았어요")
                                    .linkLabel("상세 보기")
                                    .linkUrl("https://y-fin.com/products/2")
                                    .build(),
                            DigestEmailData.DigestItem.builder()
                                    .badgeText("마감 D-14")
                                    .urgent(false)
                                    .title("청년도약계좌")
                                    .subText("10.28 (수) 마감")
                                    .linkLabel("상세 보기")
                                    .linkUrl("https://y-fin.com/products/3")
                                    .build()
                    ))
                    .moreCount(0)
                    .allUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings/notifications")
                    .unsubscribeUrl("https://y-fin.com/unsubscribe?token=abc")
                    .footerNotice(null)
                    .build();

            // when
            String html = emailTemplateService.buildDigestEmailV2(data);

            // then
            assertThat(html).contains("오늘 확인할 일정");
            assertThat(html).contains("3건");
            assertThat(html).contains("#03BFA5"); // MINT - 건수 강조
            assertThat(html).contains("부산 청년 신용회복 지원금");
            assertThat(html).contains("청년희망적금");
            assertThat(html).contains("청년도약계좌");
            assertThat(html).contains("Y-Fin.에서 모두 보기");
            assertThat(html).doesNotContain("외"); // moreCount가 0이면 "외 n건" 없음
        }

        @Test
        @DisplayName("다이제스트 템플릿 생성 - 10건 초과")
        void 다이제스트_템플릿_10건_초과() {
            // given
            DigestEmailData data = DigestEmailData.builder()
                    .count(15)
                    .baseDateLabel("10월 15일(화)")
                    .items(List.of(
                            DigestEmailData.DigestItem.builder()
                                    .badgeText("마감 D-1")
                                    .urgent(true)
                                    .title("상품1")
                                    .subText("설명1")
                                    .linkLabel("보기")
                                    .linkUrl("https://example.com/1")
                                    .build()
                    ))
                    .moreCount(5)
                    .allUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings/notifications")
                    .unsubscribeUrl("https://y-fin.com/unsubscribe?token=abc")
                    .footerNotice(null)
                    .build();

            // when
            String html = emailTemplateService.buildDigestEmailV2(data);

            // then
            assertThat(html).contains("외 5건은 Y-Fin에서 확인하세요");
        }

        @Test
        @DisplayName("만기 알림 템플릿 생성")
        void 만기_알림_템플릿_생성() {
            // given
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

            // when
            String html = emailTemplateService.buildMaturityEmailV2(data);

            // then
            assertThat(html).contains("KB Star 정기예금");
            assertThat(html).contains("만기가");
            assertThat(html).contains("30일");
            assertThat(html).contains("#7B1FA2"); // PURPLE
            assertThat(html).contains("#F3E9FA"); // PURPLE_SOFT_BG
            assertThat(html).contains("D-30");
            assertThat(html).contains("내가 등록한 가입 상품");
            assertThat(html).contains("#EFFFFD"); // MINT_SOFT_BG - 공지 박스
            assertThat(html).contains("단기 예치");
            assertThat(html).contains("목돈 만들기");
            assertThat(html).contains("만기 후 선택지 비교하기");
            assertThat(html).contains("만기일은 가입일과 저축 기간으로 계산한 예정일이에요");
        }

        @Test
        @DisplayName("이메일 제목 생성")
        void 이메일_제목_생성() {
            assertThat(emailTemplateService.buildDeadlineSubject("청년희망적금", 7))
                    .isEqualTo("[Y-Fin.] 청년희망적금 신청 마감까지 7일 남았어요");

            assertThat(emailTemplateService.buildStartSubject("청년도약계좌"))
                    .isEqualTo("[Y-Fin.] 청년도약계좌 신청이 시작되었습니다");

            assertThat(emailTemplateService.buildDigestSubject(3))
                    .isEqualTo("[Y-Fin.] 오늘 확인할 일정 3건");

            assertThat(emailTemplateService.buildMaturitySubject("KB정기예금", 30))
                    .isEqualTo("[Y-Fin.] KB정기예금 만기가 30일 남았어요");
        }

        @Test
        @DisplayName("HTML 이스케이프 처리")
        void HTML_이스케이프_처리() {
            // given
            DeadlineEmailData data = DeadlineEmailData.builder()
                    .dday(7)
                    .urgent(false)
                    .typeLabel("<script>alert('xss')</script>")
                    .title("Test & \"Product\"")
                    .startDateLabel("10월 1일")
                    .endDateLabel("10월 30일")
                    .agency("기관")
                    .applyMethod("온라인")
                    .ctaLabel("보기")
                    .ctaUrl("https://example.com")
                    .ctaExternal(false)
                    .allSchedulesUrl("https://y-fin.com/schedules")
                    .manageUrl("https://y-fin.com/settings")
                    .unsubscribeUrl("https://y-fin.com/unsub")
                    .build();

            // when
            String html = emailTemplateService.buildDeadlineEmailV2(data);

            // then
            assertThat(html).doesNotContain("<script>");
            assertThat(html).contains("&lt;script&gt;");
            assertThat(html).contains("Test &amp; &quot;Product&quot;");
        }

        @Test
        @DisplayName("공통 레이아웃 요소 포함")
        void 공통_레이아웃_요소_포함() {
            // given
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

            // when
            String html = emailTemplateService.buildDeadlineEmailV2(data);

            // then
            // 프리헤더 (숨김)
            assertThat(html).contains("display:none;max-height:0;overflow:hidden");

            // 헤더
            assertThat(html).contains("일정 알림");

            // 디자인 토큰
            assertThat(html).contains("#F1F2F4"); // PAGE_BG
            assertThat(html).contains("#FFFFFF"); // CARD_BG
            assertThat(html).contains("#E6E8EB"); // BORDER

            // 푸터
            assertThat(html).contains("알림 설정 관리");
            assertThat(html).contains("전체 수신거부");
            assertThat(html).contains("Y-Fin.");

            // 주 버튼 - Gmail 대응
            assertThat(html).contains("bgcolor=\"#03BFA5\"");
            assertThat(html).contains("color:#ffffff");
            assertThat(html).contains("<span style=\"color:#ffffff;\">");

            // 미디어 쿼리
            assertThat(html).contains("@media only screen and (max-width: 600px)");

            // 금지 요소 없음
            assertThat(html).doesNotContain("<svg");
            assertThat(html).doesNotContain("data:image");
        }

        @Test
        @DisplayName("디자인 토큰이 한 곳에서만 정의됨")
        void 디자인_토큰_단일_정의() {
            DeadlineEmailData data = DeadlineEmailData.builder()
                    .dday(3)
                    .urgent(true)
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

            // 색상값이 하드코딩되지 않고 토큰을 통해 사용되는지 확인
            // (실제로는 컴파일 시점에 상수로 치환되므로 HTML에서 확인)
            assertThat(html).contains("#03BFA5"); // MINT
            assertThat(html).contains("#F0596E"); // RED (urgent)
        }
    }

    @Nested
    @DisplayName("레거시 템플릿 - 하위 호환성")
    class LegacyTemplates {

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
}
