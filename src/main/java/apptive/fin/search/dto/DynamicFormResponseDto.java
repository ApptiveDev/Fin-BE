package apptive.fin.search.dto;

public record DynamicFormResponseDto(
    boolean showTenure,
    int yearlyEarnDefault,
    boolean showBankInterestRateCheckList,
    MedianIncomesDto medianIncomes
) {


}
