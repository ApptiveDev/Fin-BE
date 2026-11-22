package apptive.fin.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

// 부분 수정: null인 필드는 바꾸지 않는다. 그래서 이름은 null을 허용하고 공백만 막는다.
public record UserUpdateRequestDto (

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    @Pattern(regexp = ".*\\S.*", message = "이름은 공백일 수 없습니다.")
    String name

    ){
}
