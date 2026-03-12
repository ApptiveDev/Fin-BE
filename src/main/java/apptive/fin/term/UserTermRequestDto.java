package apptive.fin.term;

import lombok.Getter;
import java.util.List;

@Getter
public class UserTermRequestDto {

    // FE에 약관 넘기는 용도의 Dto
    private List<Long> termIds;
}
