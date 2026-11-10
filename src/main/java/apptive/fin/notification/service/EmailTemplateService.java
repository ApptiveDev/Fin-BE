package apptive.fin.notification.service;

import apptive.fin.notification.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final NotificationProperties properties;

    public String buildVerificationEmail(String verifyUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Pretendard', -apple-system, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 24px; font-weight: bold; color: #2563eb; }
                    .content { background: #f8fafc; border-radius: 12px; padding: 30px; }
                    .button { display: inline-block; background: #2563eb; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">Y-Fin.</div>
                    </div>
                    <div class="content">
                        <h2>이메일 인증을 완료해주세요</h2>
                        <p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요.</p>
                        <p>이 링크는 24시간 동안 유효합니다.</p>
                        <a href="%s" class="button">이메일 인증하기</a>
                        <p style="font-size: 12px; color: #64748b;">버튼이 작동하지 않으면 아래 링크를 복사하여 브라우저에 붙여넣기 해주세요:<br>%s</p>
                    </div>
                    <div class="footer">
                        <p>본 메일은 Y-Fin. 서비스 이용을 위해 발송되었습니다.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(verifyUrl, verifyUrl);
    }

    public String buildDeadlineEmail(String productName, String dDay, String detailUrl, String unsubscribeUrl) {
        return buildNotificationEmail(
                "마감 임박 알림",
                productName,
                dDay + " 마감 예정입니다.",
                "서둘러 신청하세요!",
                detailUrl,
                unsubscribeUrl
        );
    }

    public String buildStartEmail(String productName, String detailUrl, String unsubscribeUrl) {
        return buildNotificationEmail(
                "신청 시작 알림",
                productName,
                "신청이 시작되었습니다.",
                "지금 바로 확인해보세요!",
                detailUrl,
                unsubscribeUrl
        );
    }

    public String buildMaturityEmail(String productName, String dDay, String unsubscribeUrl) {
        return buildNotificationEmail(
                "만기 알림",
                productName,
                dDay + " 만기 예정입니다.",
                "만기 후 자금 계획을 세워보세요!",
                null,
                unsubscribeUrl
        );
    }

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
                    .logo { font-size: 24px; font-weight: bold; color: #2563eb; }
                    .content { background: #f8fafc; border-radius: 12px; padding: 30px; }
                    .product-name { font-size: 18px; font-weight: 600; color: #1e293b; }
                    .changes { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 16px 0; }
                    .button { display: inline-block; background: #2563eb; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
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
                    .logo { font-size: 24px; font-weight: bold; color: #2563eb; }
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

    private String buildNotificationEmail(String title, String productName, String message,
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
                    .logo { font-size: 24px; font-weight: bold; color: #2563eb; }
                    .content { background: #f8fafc; border-radius: 12px; padding: 30px; }
                    .product-name { font-size: 18px; font-weight: 600; color: #1e293b; margin: 16px 0; }
                    .message { color: #475569; }
                    .cta { color: #2563eb; font-weight: 600; }
                    .button { display: inline-block; background: #2563eb; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
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
}
