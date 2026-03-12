package apptive.fin.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserUpdateRequestDto {

    private String email;
    private String name;
}
