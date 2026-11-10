package apptive.fin.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_send_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private NotificationSchedule schedule;

    @Column(name = "digest_key", length = 100)
    private String digestKey;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "mail_type", nullable = false, length = 30)
    private MailType mailType;

    @Column(name = "resend_message_id", length = 100)
    private String resendMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailSendStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Builder
    public EmailSendLog(NotificationSchedule schedule, String digestKey, String toEmail,
                        MailType mailType, String resendMessageId, EmailSendStatus status,
                        String errorMessage) {
        this.schedule = schedule;
        this.digestKey = digestKey;
        this.toEmail = toEmail;
        this.mailType = mailType;
        this.resendMessageId = resendMessageId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.sentAt = Instant.now();
    }
}
