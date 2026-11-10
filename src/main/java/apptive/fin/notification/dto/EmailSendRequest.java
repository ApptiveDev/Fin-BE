package apptive.fin.notification.dto;

import apptive.fin.notification.entity.MailType;

public record EmailSendRequest(
        String to,
        String subject,
        String htmlBody,
        MailType mailType
) {
    public static EmailSendRequest of(String to, String subject, String htmlBody, MailType mailType) {
        return new EmailSendRequest(to, subject, htmlBody, mailType);
    }
}
