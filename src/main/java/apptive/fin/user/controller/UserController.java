package apptive.fin.user.controller;

import apptive.fin.auth.AuthUserDetails;
import apptive.fin.user.dto.UserResponseDto;
import apptive.fin.user.service.UserService;
import apptive.fin.user.dto.UserUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponseDto getMyInfo(@AuthenticationPrincipal AuthUserDetails userDetails){
        return userService.getMyInfo(userDetails.getUser().getId());
    }

    @PatchMapping("/me")
    public void updateUser(
            @RequestBody UserUpdateRequestDto request,
            @AuthenticationPrincipal AuthUserDetails userDetails
    ){
        Long userId = userDetails.getUser().getId();
        userService.updateUser(userId, request);
    }

}
