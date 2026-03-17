package apptive.fin.term.dto;


public record TermResponseDto (
    Long id,
    String title,
    String content,
    boolean isRequired,
    boolean agreed
) {
}
