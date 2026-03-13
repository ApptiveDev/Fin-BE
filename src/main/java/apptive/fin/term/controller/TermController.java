package apptive.fin.term.controller;

import apptive.fin.auth.AuthUserDetails;
import apptive.fin.term.dto.TermResponseDto;
import apptive.fin.term.service.TermService;
import apptive.fin.term.dto.UserTermRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/term")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    // 로그인 유저 기준 약관 조회
    @GetMapping
    public List<TermResponseDto> getTerms(@AuthenticationPrincipal AuthUserDetails authUserDetails){
        return termService.getTermsForUser(authUserDetails.getUser());
    }

    // 약관 동의
    @PostMapping("/agree")
    public void agreeTerms(@AuthenticationPrincipal AuthUserDetails authUserDetails,
                           @RequestBody UserTermRequestDto request){
        termService.agreeTerms(authUserDetails.getUser(), request);
    }
}
