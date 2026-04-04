package apptive.fin.search.dto;

import lombok.Builder;

@Builder
public record DynamicFormResponseDto(
    Boolean showTenure,
    Integer ageBound,
    Integer yearlyEarnDefault,
    Boolean showBankInterestRateCheckList,
    MedianIncomesDto medianIncomes
) {

    public DynamicFormResponseDto {
        if (showTenure == null) showTenure = true;
        if (ageBound == null) ageBound = 34;
        // if (yearlyEarnDefault == null);
        if (showBankInterestRateCheckList == null) showBankInterestRateCheckList = false;
        // if (medianIncomes == null) medianIncomes = null;
    }

}
