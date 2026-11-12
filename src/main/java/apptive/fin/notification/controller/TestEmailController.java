package apptive.fin.notification.controller;

import apptive.fin.notification.adapter.EmailSendPort;
import apptive.fin.notification.dto.*;
import apptive.fin.notification.entity.MailType;
import apptive.fin.notification.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Test Email", description = "테스트 이메일 발송 API (dev 전용)")
@Profile("dev")
@RestController
@RequestMapping("/test/email")
@RequiredArgsConstructor
public class TestEmailController {

    private final EmailSendPort emailSendPort;
    private final EmailTemplateService templateService;

    @Operation(summary = "테스트 이메일 발송", description = "테스트용 이메일을 발송합니다.")
    @PostMapping("/send")
    public ResponseEntity<TestEmailResponse> send(@RequestBody TestEmailRequest request) {
        String htmlBody = switch (request.templateType()) {
            case "verification" -> templateService.buildVerificationEmail("https://y-fin.com/verify?token=test");
            case "deadline" -> templateService.buildDeadlineEmail(
                    "테스트 상품", "D-3", "https://y-fin.com/products/1", "https://y-fin.com/unsubscribe"
            );
            case "start" -> templateService.buildStartEmail(
                    "테스트 상품", "https://y-fin.com/products/1", "https://y-fin.com/unsubscribe"
            );
            case "maturity" -> templateService.buildMaturityEmail(
                    "테스트 상품", "D-30", "https://y-fin.com/unsubscribe"
            );
            default -> "<h1>테스트 이메일</h1><p>Y-Fin. 테스트 이메일입니다.</p>";
        };

        EmailSendRequest sendRequest = EmailSendRequest.of(
                request.to(),
                "[Y-Fin. 테스트] " + request.templateType(),
                htmlBody,
                MailType.VERIFICATION
        );

        EmailSendResult result = emailSendPort.send(sendRequest);

        return ResponseEntity.ok(new TestEmailResponse(
                result.success(),
                result.messageId(),
                result.errorMessage()
        ));
    }

    @Operation(summary = "V2 마감 알림 테스트", description = "새 디자인 마감 알림 이메일을 발송합니다.")
    @PostMapping("/v2/deadline")
    public ResponseEntity<TestEmailResponse> sendDeadlineV2(@RequestBody SimpleEmailRequest request) {
        DeadlineEmailData data = DeadlineEmailData.builder()
                .dday(3)
                .urgent(true)
                .typeLabel("금융 지원 정책")
                .title("부산 청년 신용회복 지원금")
                .startDateLabel("10월 1일")
                .endDateLabel("10월 21일(수)")
                .agency("부산광역시")
                .applyMethod("온라인 (정부24)")
                .ctaLabel("온통청년 보기 ↗")
                .ctaUrl("https://www.youthcenter.go.kr/policy/view?seq=123")
                .ctaExternal(true)
                .allSchedulesUrl("https://y-fin.com/schedules")
                .manageUrl("https://y-fin.com/settings/notifications")
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .nextAlertDate("10.18")
                .nextAlertDday(1)
                .build();

        String htmlBody = templateService.buildDeadlineEmailV2(data);
        String subject = templateService.buildDeadlineSubject("부산 청년 신용회복 지원금", 3);

        return sendEmail(request.email(), subject, htmlBody, MailType.DEADLINE);
    }

    @Operation(summary = "V2 다이제스트 테스트", description = "새 디자인 다이제스트 이메일을 발송합니다.")
    @PostMapping("/v2/digest")
    public ResponseEntity<TestEmailResponse> sendDigestV2(@RequestBody SimpleEmailRequest request) {
        DigestEmailData data = DigestEmailData.builder()
                .count(3)
                .baseDateLabel("10월 15일(화)")
                .items(List.of(
                        DigestEmailData.DigestItem.builder()
                                .badgeText("마감 D-1")
                                .urgent(true)
                                .title("부산 청년 신용회복 지원금")
                                .subText("10.15 (목) 마감 · 내일까지 신청할 수 있어요")
                                .linkLabel("온통청년 보기")
                                .linkUrl("https://www.youthcenter.go.kr/policy/view?seq=123")
                                .build(),
                        DigestEmailData.DigestItem.builder()
                                .badgeText("마감 D-7")
                                .urgent(false)
                                .title("청년희망적금")
                                .subText("10.21 (수) 마감 · 일주일 남았어요")
                                .linkLabel("상세 보기")
                                .linkUrl("https://y-fin.com/products/2")
                                .build(),
                        DigestEmailData.DigestItem.builder()
                                .badgeText("마감 D-14")
                                .urgent(false)
                                .title("청년도약계좌")
                                .subText("10.28 (수) 마감")
                                .linkLabel("상세 보기")
                                .linkUrl("https://y-fin.com/products/3")
                                .build()
                ))
                .moreCount(0)
                .allUrl("https://y-fin.com/schedules")
                .manageUrl("https://y-fin.com/settings/notifications")
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .build();

        String htmlBody = templateService.buildDigestEmailV2(data);
        String subject = templateService.buildDigestSubject(3);

        return sendEmail(request.email(), subject, htmlBody, MailType.DIGEST);
    }

    @Operation(summary = "V2 만기 알림 테스트", description = "새 디자인 만기 알림 이메일을 발송합니다.")
    @PostMapping("/v2/maturity")
    public ResponseEntity<TestEmailResponse> sendMaturityV2(@RequestBody SimpleEmailRequest request) {
        MaturityEmailData data = MaturityEmailData.builder()
                .dday(30)
                .productName("KB Star 정기예금")
                .joinDate("2023년 11월 15일")
                .termLabel("12개월")
                .maturityDateLabel("2024년 11월 15일(금)")
                .compareUrl("https://y-fin.com/compare?type=deposit&prefill=true")
                .allSchedulesUrl("https://y-fin.com/schedules")
                .manageUrl("https://y-fin.com/settings/notifications")
                .unsubscribeUrl("https://y-fin.com/unsubscribe?token=sample")
                .build();

        String htmlBody = templateService.buildMaturityEmailV2(data);
        String subject = templateService.buildMaturitySubject("KB Star 정기예금", 30);

        return sendEmail(request.email(), subject, htmlBody, MailType.MATURITY);
    }

    private ResponseEntity<TestEmailResponse> sendEmail(String to, String subject, String htmlBody, MailType mailType) {
        EmailSendRequest sendRequest = EmailSendRequest.of(to, subject, htmlBody, mailType);
        EmailSendResult result = emailSendPort.send(sendRequest);
        return ResponseEntity.ok(new TestEmailResponse(result.success(), result.messageId(), result.errorMessage()));
    }

    public record TestEmailRequest(String to, String templateType) {}
    public record SimpleEmailRequest(String email) {}
    public record TestEmailResponse(boolean success, String messageId, String errorMessage) {}
}
