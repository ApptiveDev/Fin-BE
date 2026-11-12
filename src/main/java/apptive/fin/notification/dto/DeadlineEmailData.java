package apptive.fin.notification.dto;

import lombok.Builder;

/**
 * 마감 알림 단건 이메일 데이터
 */
@Builder
public record DeadlineEmailData(
        int dday,
        boolean urgent,
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
        String nextAlertDate,
        Integer nextAlertDday,
        String footerNotice
) {
    public String ddayText() {
        return dday == 0 ? "D-Day" : "D-" + dday;
    }

    public boolean hasNextAlert() {
        return nextAlertDate != null && nextAlertDday != null;
    }
}
