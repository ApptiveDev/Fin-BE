package apptive.fin.term.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserTermRequestDto {
    // FE에서 동의한 약관 id 목록
    private List<Long> termIds;
}
