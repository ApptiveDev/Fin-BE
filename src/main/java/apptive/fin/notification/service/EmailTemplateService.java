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
     * 마감 알림 단건 이메일 (D-7, D-3, D-1)
     */
    public String buildDeadlineEmailV2(DeadlineEmailData data) {
        Map<String, Object> context = createBaseContext();

        // D-1 여부 (내일 마감)
        context.put("isTomorrow", data.isTomorrow());

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
     * 시작 알림 이메일 (전날/당일 구분)
     */
    public String buildStartEmailV2(StartEmailData data) {
        Map<String, Object> context = createBaseContext();

        // 당일/전날 구분
        context.put("isToday", data.isToday());

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
     * 만기 알림 이메일 (D-30, D-7)
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
        alert.put("daysLeftText", data.daysLeftText());  // 7일일 때 "일주일"
        alert.put("isOneWeek", data.isOneWeek());
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
        if (dday == 1) {
            return MAIL_SUBJECT_PREFIX + " 내일 " + productName + " 신청이 마감돼요";
        }
        return MAIL_SUBJECT_PREFIX + " " + productName + " 신청 마감까지 " + dday + "일 남았어요";
    }

    public String buildStartSubject(String productName, boolean isToday) {
        if (isToday) {
            return MAIL_SUBJECT_PREFIX + " " + productName + " 신청이 오늘 시작됐어요";
        }
        return MAIL_SUBJECT_PREFIX + " 내일 " + productName + " 신청이 시작돼요";
    }

    public String buildDigestSubject(int count) {
        return MAIL_SUBJECT_PREFIX + " 오늘 확인할 일정 " + count + "건";
    }

    public String buildMaturitySubject(String productName, int dday) {
        String daysText = (dday == 7) ? "일주일" : dday + "일";
        return MAIL_SUBJECT_PREFIX + " " + productName + " 만기가 " + daysText + " 남았어요";
    }

    public String buildCorrectionSubject(String productName) {
        return MAIL_SUBJECT_PREFIX + " " + productName + " 일정이 변경됐어요";
    }

    public String buildVerificationSubject() {
        return MAIL_SUBJECT_PREFIX + " 알림 받을 이메일을 확인해 주세요";
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
