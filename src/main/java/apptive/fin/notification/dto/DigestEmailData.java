package apptive.fin.notification.dto;

import lombok.Builder;

import java.util.List;

/**
 * 다이제스트(묶음 알림) 이메일 데이터
 */
@Builder
public record DigestEmailData(
        int count,
        String baseDateLabel,
        List<DigestItem> items,
        int moreCount,
        String allUrl,
        String manageUrl,
        String unsubscribeUrl,
        String footerNotice
) {
    @Builder
    public record DigestItem(
            String badgeText,
            boolean urgent,
            String title,
            String subText,
            String linkLabel,
            String linkUrl
    ) {}
}
