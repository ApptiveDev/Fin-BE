package apptive.fin.notification;

import apptive.fin.notification.dto.DeadlineEmailData;
import apptive.fin.notification.dto.DigestEmailData;
import apptive.fin.notification.dto.MaturityEmailData;
import apptive.fin.notification.dto.StartEmailData;
import apptive.fin.notification.service.EmailTemplateService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 이메일 템플릿 HTML 프리뷰 생성기
 * 실행: ./gradlew test --tests "EmailTemplatePreviewGenerator.generatePreviews"
 * 또는 IDE에서 직접 실행
 */
public class EmailTemplatePreviewGenerator {

    private static final Path OUTPUT_DIR = Path.of("build/email-preview");

    public static void main(String[] args) throws IOException {
        generatePreviews();
    }

    public static void generatePreviews() throws IOException {
        EmailTemplateService service = new EmailTemplateService(null);

        Files.createDirectories(OUTPUT_DIR);

        // 1. deadline-single (긴급)
        DeadlineEmailData deadlineUrgent = DeadlineEmailData.builder()
                .dday(3)
                .urgent(true)
                .typeLabel("금융 지원 정책")
                .title("부산 청년 신용회복 지원금")
                .startDateLabel("10월 1일")
                .endDateLabel("10월 21일(수)")
                .agency("부산광역시")
                .applyMethod("온라인 (정부24)")
                .ctaLabel("온통청년 보기 ↗")
                .ctaUrl("https://www.youthcenter.go.kr/policy/view?seq=123")
                .ctaExternal(true)
                .allSchedulesUrl("https://y-fin.com/schedules")
                .manageUrl("https://y-fin.com/settings/notifications")
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .nextAlertDate("10.18")
                .nextAlertDday(1)
                .footerNotice(null)
                .build();
        writeHtml("deadline-urgent.html", service.buildDeadlineEmailV2(deadlineUrgent));

        // 2. deadline-single (여유)
        DeadlineEmailData deadlineNormal = DeadlineEmailData.builder()
                .dday(7)
                .urgent(false)
                .typeLabel("금융 지원 정책")
                .title("청년희망적금")
                .startDateLabel("9월 15일")
                .endDateLabel("10월 30일(목)")
                .agency("금융위원회")
                .applyMethod("은행 앱 또는 방문")
                .ctaLabel("상세 보기")
                .ctaUrl("https://y-fin.com/products/1")
                .ctaExternal(false)
                .allSchedulesUrl("https://y-fin.com/schedules")
                .manageUrl("https://y-fin.com/settings/notifications")
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .nextAlertDate(null)
                .nextAlertDday(null)
                .footerNotice(null)
                .build();
        writeHtml("deadline-normal.html", service.buildDeadlineEmailV2(deadlineNormal));

        // 3. start (시작 알림)
        StartEmailData start = StartEmailData.builder()
                .typeLabel("금융 지원 정책")
                .title("청년도약계좌")
                .startDateLabel("10월 1일")
                .endDateLabel("12월 31일(화)")
                .agency("금융위원회")
                .applyMethod("은행 앱")
                .ctaLabel("온통청년 보기 ↗")
                .ctaUrl("https://www.youthcenter.go.kr/policy/view?seq=456")
                .ctaExternal(true)
                .allSchedulesUrl("https://y-fin.com/schedules")
                .manageUrl("https://y-fin.com/settings/notifications")
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .footerNotice(null)
                .build();
        writeHtml("start.html", service.buildStartEmailV2(start));

        // 4. digest (다이제스트)
        DigestEmailData digest = DigestEmailData.builder()
                .count(3)
                .baseDateLabel("10월 15일(화)")
                .items(List.of(
                        DigestEmailData.DigestItem.builder()
                                .badgeText("마감 D-1")
                                .urgent(true)
                                .title("부산 청년 신용회복 지원금")
                                .subText("10.15 (목) 마감 · 내일까지 신청할 수 있어요")
                                .linkLabel("온통청년 보기")
                                .linkUrl("https://www.youthcenter.go.kr/policy/view?seq=123")
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
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .footerNotice(null)
                .build();
        writeHtml("digest.html", service.buildDigestEmailV2(digest));

        // 5. digest (10건 초과)
        DigestEmailData digestMany = DigestEmailData.builder()
                .count(12)
                .baseDateLabel("10월 15일(화)")
                .items(List.of(
                        DigestEmailData.DigestItem.builder()
                                .badgeText("마감 D-1")
                                .urgent(true)
                                .title("상품 1")
                                .subText("10.15 마감")
                                .linkLabel("보기")
                                .linkUrl("https://example.com/1")
                                .build(),
                        DigestEmailData.DigestItem.builder()
                                .badgeText("마감 D-3")
                                .urgent(true)
                                .title("상품 2")
                                .subText("10.17 마감")
                                .linkLabel("보기")
                                .linkUrl("https://example.com/2")
                                .build()
                ))
                .moreCount(10)
                .allUrl("https://y-fin.com/schedules")
                .manageUrl("https://y-fin.com/settings/notifications")
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .footerNotice(null)
                .build();
        writeHtml("digest-many.html", service.buildDigestEmailV2(digestMany));

        // 6. maturity (만기 알림)
        MaturityEmailData maturity = MaturityEmailData.builder()
                .dday(30)
                .productName("KB Star 정기예금")
                .joinDate("2023년 11월 15일")
                .termLabel("12개월")
                .maturityDateLabel("2024년 11월 15일(금)")
                .compareUrl("https://y-fin.com/compare?type=deposit&prefill=true")
                .allSchedulesUrl("https://y-fin.com/schedules")
                .manageUrl("https://y-fin.com/settings/notifications")
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .footerNotice(null)
                .build();
        writeHtml("maturity.html", service.buildMaturityEmailV2(maturity));

        System.out.println("Email previews generated in: " + OUTPUT_DIR.toAbsolutePath());
        Files.list(OUTPUT_DIR).forEach(p -> System.out.println("  - " + p.getFileName()));
    }

    private static void writeHtml(String filename, String html) throws IOException {
        Files.writeString(OUTPUT_DIR.resolve(filename), html);
    }
}
