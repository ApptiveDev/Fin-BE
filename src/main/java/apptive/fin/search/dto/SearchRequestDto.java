package apptive.fin.search.dto;

import jakarta.validation.Valid;

import java.util.List;

public record SearchRequestDto(
        List<@Valid OptionRequestDto> options,
        DetailedOptionsDto detailedOptions
) {
}
