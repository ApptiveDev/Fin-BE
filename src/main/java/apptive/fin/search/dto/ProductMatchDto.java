package apptive.fin.search.dto;

import lombok.Builder;

@Builder
public record ProductMatchDto(
        String productId,
        String productName,
        String source, // government 또는 bank
        double totalScore,

        double benefitScore,
        double periodScore,
        double identityScore,
        double depositScore,
        double bankCondScore

) {}
