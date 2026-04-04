package apptive.fin.search.dto;

public record MedianIncomesDto(
        Integer year,
        Integer householdSize,
        Integer p60,
        Integer p80,
        Integer p100,
        Integer p120,
        Integer p150,
        Integer p180
) {
}
