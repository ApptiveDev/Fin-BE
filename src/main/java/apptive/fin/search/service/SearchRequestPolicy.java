package apptive.fin.search.service;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.global.error.BusinessException;
import apptive.fin.search.SearchErrorCode;
import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.ResolvedKeywords;
import apptive.fin.search.dto.SearchRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchRequestPolicy {

    public void validateForRecommendation(SearchRequestDto request, ResolvedKeywords keywords) {
        if (request == null || request.monthlySavingsGoal() == null) {
            throw new BusinessException(SearchErrorCode.MONTHLY_SAVINGS_GOAL_REQUIRED);
        }
        if (keywords == null || keywords.savingPeriod() == null) {
            throw new BusinessException(SearchErrorCode.SAVING_PERIOD_REQUIRED);
        }
        if (keywords.bankConditions() == null || keywords.bankConditions().isEmpty()) {
            throw new BusinessException(SearchErrorCode.BANK_CONDITION_REQUIRED);
        }
    }

    /** 첫거래/만기예적금 은행은 최소 1개 이상 선택해야 입력을 완료한 것으로 본다. 빈 목록과 null은 둘 다 미입력으로 본다. */
    public boolean canUsePersonalization(
            SearchRequestDto request,
            ResolvedKeywords keywords,
            AuthUserDetails userDetails
    ) {
        if (userDetails == null
                || !userDetails.getRole().canUseRecommendation()
                || !isStep1Complete(request, keywords)) {
            return false;
        }

        DetailedOptionsDto detail = request.detailedOptions();
        return detail != null
                && detail.birthdate() != null
                && detail.annualIncome() != null
                && detail.householdSize() != null
                && detail.householdIncomePercent() != null
                && detail.neverUsedBanks() != null
                && detail.maturedSavingBanks() != null;
    }


    private boolean isStep1Complete(SearchRequestDto request, ResolvedKeywords keywords) {
        return request != null
                && request.monthlySavingsGoal() != null
                && keywords != null
                && keywords.savingPeriod() != null
                && keywords.bankConditions() != null
                && !keywords.bankConditions().isEmpty();
    }
}
