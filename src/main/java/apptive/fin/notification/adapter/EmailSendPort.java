package apptive.fin.notification.adapter;

import apptive.fin.notification.dto.EmailSendRequest;
import apptive.fin.notification.dto.EmailSendResult;

public interface EmailSendPort {

    EmailSendResult send(EmailSendRequest request);
}
