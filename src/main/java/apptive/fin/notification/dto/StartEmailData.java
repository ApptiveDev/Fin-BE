package apptive.fin.notification.dto;

import lombok.Builder;

/**
 * 시작 알림 이메일 데이터 (deadline-single 구조 재사용, 민트색 강조)
 */
@Builder
public record StartEmailData(
        String typeLabel,
        String title,
        String startDateLabel,
        String endDateLabel,
        String agency,
        String applyMethod,
        String ctaLabel,
        String ctaUrl,
        boolean ctaExternal,
        String allSchedulesUrl,
        String manageUrl,
        String unsubscribeUrl,
        String footerNotice
) {}
