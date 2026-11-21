package apptive.fin.notification.controller;

import apptive.fin.notification.service.UnsubscribeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Unsubscribe", description = "수신거부 API")
@RestController
@RequestMapping("/notifications/unsubscribe")
@RequiredArgsConstructor
public class UnsubscribeController {

    private final UnsubscribeService unsubscribeService;

    @Operation(summary = "수신거부", description = "이메일 수신을 거부합니다.")
    @GetMapping
    public ResponseEntity<String> unsubscribe(
            @RequestParam String email,
            @RequestParam String sig
    ) {
        unsubscribeService.unsubscribe(email, sig);
        return ResponseEntity.ok("수신거부가 완료되었습니다. 더 이상 알림 이메일을 받지 않습니다.");
    }

    @Operation(summary = "수신거부 해제", description = "이메일 수신거부를 해제합니다.")
    @PostMapping("/cancel")
    public ResponseEntity<String> resubscribe(@RequestParam String email) {
        unsubscribeService.resubscribe(email);
        return ResponseEntity.ok("수신거부가 해제되었습니다.");
    }
}
