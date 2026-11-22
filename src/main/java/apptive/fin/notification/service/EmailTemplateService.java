package apptive.fin.notification.service;

import apptive.fin.notification.NotificationProperties;
import apptive.fin.notification.dto.DeadlineEmailData;
import apptive.fin.notification.dto.DigestEmailData;
import apptive.fin.notification.dto.MaturityEmailData;
import apptive.fin.notification.dto.StartEmailData;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private static final String MAIL_SUBJECT_PREFIX = "[Y-Fin.]";

    private final NotificationProperties properties;
    private final Mustache.Compiler mustacheCompiler = Mustache.compiler();

    // ──────────────────────────────────────────────────────────────────────────
    // Public API - Mustache 기반 템플릿
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 마감 알림 단건 이메일 (D-7)
     */
    public String buildDeadlineEmailV2(DeadlineEmailData data) {
        Map<String, Object> context = createBaseContext();

        // policy 객체
        Map<String, Object> policy = new HashMap<>();
        policy.put("name", data.title());
        policy.put("applyStartText", data.startDateLabel());
        policy.put("applyEndText", data.endDateLabel());
        policy.put("applyPeriod", data.startDateLabel() + " ~ " + data.endDateLabel());
        policy.put("institution", data.agency());
        policy.put("applyMethod", data.applyMethod());
        policy.put("externalUrl", data.ctaUrl());
        context.put("policy", policy);

        // alert 객체
        Map<String, Object> alert = new HashMap<>();
        alert.put("daysLeft", data.dday());
        if (data.nextAlertDate() != null && data.nextAlertDday() != null) {
            alert.put("nextAlertText", data.nextAlertDate() + " (D-" + data.nextAlertDday() + ")");
        }
        context.put("alert", alert);

        // links 객체
        Map<String, Object> links = new HashMap<>();
        links.put("schedule", data.allSchedulesUrl());
        links.put("settings", data.manageUrl());
        links.put("unsubscribe", data.unsubscribeUrl());
        context.put("links", links);

        return render("templates/email/deadline.html", context);
    }

    /**
     * 시작 알림 이메일
     */
    public String buildStartEmailV2(StartEmailData data) {
        Map<String, Object> context = createBaseContext();

        Map<String, Object> policy = new HashMap<>();
        policy.put("name", data.title());
        policy.put("applyStartText", data.startDateLabel());
        policy.put("applyEndText", data.endDateLabel());
        policy.put("applyPeriod", data.startDateLabel() + " ~ " + data.endDateLabel());
        policy.put("institution", data.agency());
        policy.put("applyMethod", data.applyMethod());
        policy.put("externalUrl", data.ctaUrl());
        context.put("policy", policy);

        Map<String, Object> links = new HashMap<>();
        links.put("schedule", data.allSchedulesUrl());
        links.put("settings", data.manageUrl());
        links.put("unsubscribe", data.unsubscribeUrl());
        context.put("links", links);

        return render("templates/email/start.html", context);
    }

    /**
     * 다이제스트 이메일
     */
    public String buildDigestEmailV2(DigestEmailData data) {
        Map<String, Object> context = createBaseContext();

        context.put("itemCount", data.count());
        context.put("baseDateText", data.baseDateLabel());

        // schedules 리스트 변환
        var schedules = data.items().stream().map(item -> {
            Map<String, Object> schedule = new HashMap<>();
            schedule.put("daysLeft", extractDaysLeft(item.badgeText()));
            schedule.put("badgeBg", item.urgent() ? "#EF5B6E" : "#fcf4f5");
            schedule.put("badgeColor", item.urgent() ? "#ffffff" : "#ef5b6e");
            schedule.put("name", item.title());
            schedule.put("deadlineText", item.subText());
            schedule.put("externalUrl", item.linkUrl());
            return schedule;
        }).toList();
        context.put("schedules", schedules);

        Map<String, Object> links = new HashMap<>();
        links.put("schedule", data.allUrl());
        links.put("settings", data.manageUrl());
        links.put("unsubscribe", data.unsubscribeUrl());
        context.put("links", links);

        return render("templates/email/digest.html", context);
    }

    /**
     * 만기 알림 이메일 (D-30)
     */
    public String buildMaturityEmailV2(MaturityEmailData data) {
        Map<String, Object> context = createBaseContext();

        Map<String, Object> product = new HashMap<>();
        product.put("name", data.productName());
        product.put("maturityDateLong", data.maturityDateLabel());
        product.put("joinDate", data.joinDate());
        product.put("saveMonths", extractMonths(data.termLabel()));
        product.put("maturityDate", data.maturityDateLabel());
        context.put("product", product);

        Map<String, Object> alert = new HashMap<>();
        alert.put("daysLeft", data.dday());
        context.put("alert", alert);

        Map<String, Object> links = new HashMap<>();
        links.put("compare", data.compareUrl());
        links.put("settings", data.manageUrl());
        links.put("unsubscribe", data.unsubscribeUrl());
        context.put("links", links);

        return render("templates/email/maturity.html", context);
    }

    /**
     * 인증 이메일
     */
    public String buildVerificationEmail(String verifyUrl) {
        Map<String, Object> context = createBaseContext();

        Map<String, Object> links = new HashMap<>();
        links.put("verify", verifyUrl);
        context.put("links", links);

        return render("templates/email/verification.html", context);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Subject 생성
    // ──────────────────────────────────────────────────────────────────────────

    public String buildDeadlineSubject(String productName, int dday) {
        return MAIL_SUBJECT_PREFIX + " " + productName + " 신청 마감까지 " + dday + "일 남았어요";
    }

    public String buildStartSubject(String productName) {
        return MAIL_SUBJECT_PREFIX + " " + productName + " 신청이 시작되었습니다";
    }

    public String buildDigestSubject(int count) {
        return MAIL_SUBJECT_PREFIX + " 오늘 확인할 일정 " + count + "건";
    }

    public String buildMaturitySubject(String productName, int dday) {
        return MAIL_SUBJECT_PREFIX + " " + productName + " 만기가 " + dday + "일 남았어요";
    }

    public String buildVerificationSubject() {
        return MAIL_SUBJECT_PREFIX + " 알림 받을 이메일을 확인해 주세요";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 기존 API - 하위 호환성 유지 (추후 제거 예정)
    // ──────────────────────────────────────────────────────────────────────────

    @Deprecated
    public String buildDeadlineEmail(String productName, String dDay, String detailUrl, String unsubscribeUrl) {
        return buildNotificationEmailLegacy(
                "마감 임박 알림",
                productName,
                dDay + " 마감 예정입니다.",
                "서둘러 신청하세요!",
                detailUrl,
                unsubscribeUrl
        );
    }

    @Deprecated
    public String buildStartEmail(String productName, String detailUrl, String unsubscribeUrl) {
        return buildNotificationEmailLegacy(
                "신청 시작 알림",
                productName,
                "신청이 시작되었습니다.",
                "지금 바로 확인해보세요!",
                detailUrl,
                unsubscribeUrl
        );
    }

    @Deprecated
    public String buildMaturityEmail(String productName, String dDay, String unsubscribeUrl) {
        return buildNotificationEmailLegacy(
                "만기 알림",
                productName,
                dDay + " 만기 예정입니다.",
                "만기 후 자금 계획을 세워보세요!",
                null,
                unsubscribeUrl
        );
    }

    @Deprecated
    public String buildCorrectionEmail(String productName, String changeDetails, String detailUrl, String unsubscribeUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Pretendard', -apple-system, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 24px; font-weight: bold; color: #03BFA5; }
                    .content { background: #f8fafc; border-radius: 12px; padding: 30px; }
                    .product-name { font-size: 18px; font-weight: 600; color: #1e293b; }
                    .changes { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 16px 0; }
                    .button { display: inline-block; background: #03BFA5; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">Y-Fin.</div>
                    </div>
                    <div class="content">
                        <h2>상품 정보 변경 알림</h2>
                        <p class="product-name">%s</p>
                        <div class="changes">%s</div>
                        <a href="%s" class="button">상세 확인하기</a>
                    </div>
                    <div class="footer">
                        <p><a href="%s">수신거부</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(productName, changeDetails, detailUrl, unsubscribeUrl);
    }

    @Deprecated
    public String buildDigestEmail(String digestContent, String unsubscribeUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Pretendard', -apple-system, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 24px; font-weight: bold; color: #03BFA5; }
                    .content { background: #f8fafc; border-radius: 12px; padding: 30px; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">Y-Fin.</div>
                    </div>
                    <div class="content">
                        <h2>오늘의 알림 요약</h2>
                        %s
                    </div>
                    <div class="footer">
                        <p><a href="%s">수신거부</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(digestContent, unsubscribeUrl);
    }

    @Deprecated
    private String buildNotificationEmailLegacy(String title, String productName, String message,
                                                String callToAction, String detailUrl, String unsubscribeUrl) {
        String buttonHtml = detailUrl != null
                ? "<a href=\"" + detailUrl + "\" class=\"button\">상세 확인하기</a>"
                : "";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Pretendard', -apple-system, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 24px; font-weight: bold; color: #03BFA5; }
                    .content { background: #f8fafc; border-radius: 12px; padding: 30px; }
                    .product-name { font-size: 18px; font-weight: 600; color: #1e293b; margin: 16px 0; }
                    .message { color: #475569; }
                    .cta { color: #03BFA5; font-weight: 600; }
                    .button { display: inline-block; background: #03BFA5; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">Y-Fin.</div>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        <p class="product-name">%s</p>
                        <p class="message">%s</p>
                        <p class="cta">%s</p>
                        %s
                    </div>
                    <div class="footer">
                        <p><a href="%s">수신거부</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, productName, message, callToAction, buttonHtml, unsubscribeUrl);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, Object> createBaseContext() {
        Map<String, Object> context = new HashMap<>();

        // assets 객체
        Map<String, Object> assets = new HashMap<>();
        assets.put("logoUrl", properties.email().logoUrl());
        context.put("assets", assets);

        // company 객체
        Map<String, Object> company = new HashMap<>();
        company.put("address", properties.company().address());
        company.put("supportEmail", properties.company().supportEmail());
        context.put("company", company);

        return context;
    }

    private String render(String templatePath, Map<String, Object> context) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                Template template = mustacheCompiler.compile(reader);
                return template.execute(context);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to render template: " + templatePath, e);
        }
    }

    private int extractDaysLeft(String badgeText) {
        // "마감 D-7" 형태에서 숫자 추출
        if (badgeText == null) return 0;
        try {
            return Integer.parseInt(badgeText.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractMonths(String termLabel) {
        // "12개월" 형태에서 숫자만 추출
        if (termLabel == null) return "0";
        return termLabel.replaceAll("[^0-9]", "");
    }
}
