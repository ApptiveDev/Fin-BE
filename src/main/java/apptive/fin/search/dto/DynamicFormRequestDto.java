package apptive.fin.search.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record DynamicFormRequestDto(
     List<@Valid Option> options
) {

    public record Option(
            @NotBlank String categoryName,
            @NotBlank String optionName,
            JsonNode optionValue
    ) {}
}
