package apptive.fin.notification.repository;

import apptive.fin.notification.entity.EmailSendLog;
import apptive.fin.notification.entity.MailType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EmailSendLogRepository extends JpaRepository<EmailSendLog, Long> {

    List<EmailSendLog> findByToEmailOrderBySentAtDesc(String toEmail);

    List<EmailSendLog> findByMailTypeAndSentAtAfter(MailType mailType, Instant after);
}
