package apptive.fin.user.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/me")
    public String getMyInfo() {
        return "내 정보 조회";
    }

}
