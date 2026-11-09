package apptive.fin.search.dto;

import apptive.fin.global.util.AgeUtil;
import apptive.fin.search.enums.ProductCategoryEnum;

import java.time.LocalDate;
import java.util.List;

public record DetailedOptionsDto(
        LocalDate birthdate,
        Long annualIncome,
        Integer householdSize,
        Integer householdIncomePercent,
        Integer tenureMonths,
        Boolean isFirstJob,
        Boolean isHomeless,
        Boolean isHouseholder, // 세대주 여부
        Long monthlySavingsGoal,       // 월 저축 가능액 (목돈만들기용, 단위: 원)
        Long depositAmount,            // 예치 희망액 (단기예치용, 단위: 원)
        List<String> neverUsedBanks,
        List<String> maturedSavingBanks,
        List<PreferentialInterestRateOption> selectedInterestRateOptions
) {
    // 레거시 호환용 생성자 (거래이력 없음)
    public DetailedOptionsDto(
            LocalDate birthdate,
            Long annualIncome,
            Integer householdSize,
            Integer householdIncomePercent,
            Integer tenureMonths,
            Boolean isFirstJob,
            Boolean isHomeless,
            Boolean isHouseholder,
            Long monthlySavingsGoal,
            List<PreferentialInterestRateOption> selectedInterestRateOptions
    ) {
        this(
                birthdate,
                annualIncome,
                householdSize,
                householdIncomePercent,
                tenureMonths,
                isFirstJob,
                isHomeless,
                isHouseholder,
                monthlySavingsGoal,
                null,  // depositAmount
                null,  // neverUsedBanks
                null,  // maturedSavingBanks
                selectedInterestRateOptions
        );
    }

    // 레거시 호환용 생성자 (거래이력 포함, 신규 필드 없음)
    public DetailedOptionsDto(
            LocalDate birthdate,
            Long annualIncome,
            Integer householdSize,
            Integer householdIncomePercent,
            Integer tenureMonths,
            Boolean isFirstJob,
            Boolean isHomeless,
            Boolean isHouseholder,
            Long monthlySavingsGoal,
            List<String> neverUsedBanks,
            List<String> maturedSavingBanks,
            List<PreferentialInterestRateOption> selectedInterestRateOptions
    ) {
        this(
                birthdate,
                annualIncome,
                householdSize,
                householdIncomePercent,
                tenureMonths,
                isFirstJob,
                isHomeless,
                isHouseholder,
                monthlySavingsGoal,
                null,  // depositAmount
                neverUsedBanks,
                maturedSavingBanks,
                selectedInterestRateOptions
        );
    }

    // 기준일(today) 시점의 만 나이. 생일 미입력 시 null.
    public Integer age(LocalDate today) {
        return AgeUtil.age(birthdate, today);
    }

    public Integer age() {
        return AgeUtil.age(birthdate);
    }

    /**
     * 적금 상품 하나에 넣을 월 납입액.
     * 단기예치는 예치액을 상품 기간으로 나누고, 목돈만들기는 월 저축 가능액을 그대로 쓴다. 계산할 수 없으면 null.
     */
    public Long monthlyDepositFor(ProductCategoryEnum category, Integer productSaveTrm) {
        if (category == ProductCategoryEnum.SHORT_TERM) {
            return depositAmount != null && productSaveTrm != null && productSaveTrm > 0
                    ? depositAmount / productSaveTrm
                    : null;
        }
        return monthlySavingsGoal;
    }

    /**
     * 예금 상품 하나에 넣을 예치 원금.
     * 단기예치는 예치액을 그대로 쓰고, 목돈만들기는 월 저축 가능액에 상품 기간을 곱한다. 계산할 수 없으면 null.
     */
    public Long depositPrincipalFor(ProductCategoryEnum category, Integer productSaveTrm) {
        if (category == ProductCategoryEnum.SHORT_TERM) {
            return depositAmount;
        }
        return monthlySavingsGoal != null && productSaveTrm != null
                ? monthlySavingsGoal * productSaveTrm
                : null;
    }
}
