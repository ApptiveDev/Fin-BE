package apptive.fin.notification.dto;

import lombok.Builder;

/**
 * 만기 알림 이메일 데이터
 */
@Builder
public record MaturityEmailData(
        int dday,
        String productName,
        String joinDate,
        String termLabel,
        String maturityDateLabel,
        String compareUrl,
        String allSchedulesUrl,
        String manageUrl,
        String unsubscribeUrl,
        String footerNotice
) {
    public String ddayText() {
        return dday == 0 ? "D-Day" : "D-" + dday;
    }
}
