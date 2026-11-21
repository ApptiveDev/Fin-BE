package apptive.fin.notification.service;

import apptive.fin.notification.NotificationProperties;
import apptive.fin.notification.dto.DeadlineEmailData;
import apptive.fin.notification.dto.DigestEmailData;
import apptive.fin.notification.dto.MaturityEmailData;
import apptive.fin.notification.dto.StartEmailData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    // TODO: 시안은 "[Y-Fin.]", 명세는 "[Y-Fin]" - 확정 후 통일 필요
    private static final String MAIL_SUBJECT_PREFIX = "[Y-Fin.]";

    private final TemplateEngine templateEngine;
    private final NotificationProperties properties;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API - Thymeleaf 기반 템플릿
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 마감 알림 단건 이메일
     */
    public String buildDeadlineEmailV2(DeadlineEmailData data) {
        Context context = createBaseContext();

        context.setVariable("preheader", data.title() + " 신청 마감까지 " + data.dday() + "일 남았어요");
        context.setVariable("dday", data.dday());
        context.setVariable("urgent", data.urgent());
        context.setVariable("typeLabel", data.typeLabel());
        context.setVariable("title", data.title());
        context.setVariable("startDateLabel", data.startDateLabel());
        context.setVariable("endDateLabel", data.endDateLabel());
        context.setVariable("agency", data.agency());
        context.setVariable("applyMethod", data.applyMethod());
        context.setVariable("ctaLabel", data.ctaLabel());
        context.setVariable("ctaUrl", data.ctaUrl());
        context.setVariable("ctaExternal", data.ctaExternal());
        context.setVariable("allSchedulesUrl", data.allSchedulesUrl());
        context.setVariable("manageUrl", data.manageUrl());
        context.setVariable("unsubscribeUrl", data.unsubscribeUrl());
        context.setVariable("nextAlertDate", data.nextAlertDate());
        context.setVariable("nextAlertDday", data.nextAlertDday());
        context.setVariable("footerNotice", data.footerNotice());

        return templateEngine.process("email/deadline", context);
    }

    /**
     * 시작 알림 이메일
     */
    public String buildStartEmailV2(StartEmailData data) {
        Context context = createBaseContext();

        context.setVariable("preheader", data.title() + " 신청이 시작되었습니다");
        context.setVariable("typeLabel", data.typeLabel());
        context.setVariable("title", data.title());
        context.setVariable("startDateLabel", data.startDateLabel());
        context.setVariable("endDateLabel", data.endDateLabel());
        context.setVariable("agency", data.agency());
        context.setVariable("applyMethod", data.applyMethod());
        context.setVariable("ctaLabel", data.ctaLabel());
        context.setVariable("ctaUrl", data.ctaUrl());
        context.setVariable("ctaExternal", data.ctaExternal());
        context.setVariable("allSchedulesUrl", data.allSchedulesUrl());
        context.setVariable("manageUrl", data.manageUrl());
        context.setVariable("unsubscribeUrl", data.unsubscribeUrl());
        context.setVariable("footerNotice", data.footerNotice());

        return templateEngine.process("email/start", context);
    }

    /**
     * 다이제스트 이메일
     */
    public String buildDigestEmailV2(DigestEmailData data) {
        Context context = createBaseContext();

        context.setVariable("preheader", "오늘 확인할 일정 " + data.count() + "건");
        context.setVariable("count", data.count());
        context.setVariable("baseDateLabel", data.baseDateLabel());
        context.setVariable("items", data.items());
        context.setVariable("moreCount", data.moreCount());
        context.setVariable("allUrl", data.allUrl());
        context.setVariable("manageUrl", data.manageUrl());
        context.setVariable("unsubscribeUrl", data.unsubscribeUrl());
        context.setVariable("footerNotice", data.footerNotice());

        return templateEngine.process("email/digest", context);
    }

    /**
     * 만기 알림 이메일
     */
    public String buildMaturityEmailV2(MaturityEmailData data) {
        Context context = createBaseContext();

        context.setVariable("preheader", data.productName() + " 만기가 " + data.dday() + "일 남았어요");
        context.setVariable("dday", data.dday());
        context.setVariable("productName", data.productName());
        context.setVariable("joinDate", data.joinDate());
        context.setVariable("termLabel", data.termLabel());
        context.setVariable("maturityDateLabel", data.maturityDateLabel());
        context.setVariable("compareUrl", data.compareUrl());
        context.setVariable("allSchedulesUrl", data.allSchedulesUrl());
        context.setVariable("manageUrl", data.manageUrl());
        context.setVariable("unsubscribeUrl", data.unsubscribeUrl());
        context.setVariable("footerNotice", data.footerNotice());

        return templateEngine.process("email/maturity", context);
    }

    /**
     * 인증 이메일
     */
    public String buildVerificationEmail(String verifyUrl) {
        Context context = createBaseContext();

        context.setVariable("preheader", "Y-Fin. 이메일 인증을 완료해주세요");
        context.setVariable("verifyUrl", verifyUrl);

        return templateEngine.process("email/verification", context);
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
        return MAIL_SUBJECT_PREFIX + " 이메일 인증을 완료해주세요";
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

    private Context createBaseContext() {
        Context context = new Context(Locale.KOREA);
        // TODO: properties에서 사업자 정보 읽기
        context.setVariable("businessInfo", null);
        context.setVariable("logoUrl", "https://y-fin.com/logo-email.png");
        return context;
    }
}
