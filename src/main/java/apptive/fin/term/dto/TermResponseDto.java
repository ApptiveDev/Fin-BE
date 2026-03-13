package apptive.fin.term.dto;

import lombok.Getter;

@Getter
public class TermResponseDto {
    private Long id;
    private String title;
    private String content;
    private boolean isRequired;
    private boolean agreed;

    public TermResponseDto(Long id, String title, String content, boolean isRequired, boolean agreed) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.isRequired = isRequired;
        this.agreed = agreed;
    }


}
