package apptive.fin.notification.scheduler;

import apptive.fin.notification.adapter.EmailSendPort;
import apptive.fin.notification.dto.EmailSendRequest;
import apptive.fin.notification.dto.EmailSendResult;
import apptive.fin.notification.scheduler.NotificationScheduleTxHelper.EmailSendContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 알림 스케줄 처리 메인 클래스.
 * 트랜잭션 메서드는 NotificationScheduleTxHelper로 분리하여 Spring AOP 프록시 적용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduleProcessor {

    private final NotificationScheduleTxHelper txHelper;
    private final EmailSendPort emailSendPort;

    /**
     * 스케줄 처리 메인 메서드 (트랜잭션 분리)
     *
     * [TX 1] SCHEDULED → SENDING 상태 변경으로 작업 선점
     * [트랜잭션 없음] 외부 서비스(이메일) 호출
     * [TX 2] 결과에 따라 SENT/FAILED/SKIPPED 상태 변경 및 로그 저장
     */
    public void processSchedule(Long scheduleId) {
        // [TX 1] 작업 선점 - SCHEDULED → SENDING (별도 빈 호출로 프록시 경유)
        EmailSendContext context = txHelper.claimSchedule(scheduleId);
        if (context == null) {
            return; // 이미 다른 프로세스가 처리 중이거나 스킵됨
        }

        // [트랜잭션 없음] 이메일 전송
        EmailSendResult result = null;
        if (context.shouldSend()) {
            result = sendEmail(context);
        }

        // [TX 2] 결과 저장 (별도 빈 호출로 프록시 경유)
        txHelper.completeSchedule(scheduleId, context, result);
    }

    private EmailSendResult sendEmail(EmailSendContext context) {
        EmailSendRequest request = EmailSendRequest.of(
                context.email(), context.subject(), context.htmlBody(), context.mailType());
        return emailSendPort.send(request);
    }
}
