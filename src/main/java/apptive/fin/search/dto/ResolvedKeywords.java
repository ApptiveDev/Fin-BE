package apptive.fin.search.dto;

import apptive.fin.search.enums.KeywordValueEnum;
import apptive.fin.search.enums.ProductCategoryEnum;

import java.util.List;

public record ResolvedKeywords(
        List<KeywordValueEnum> regions,
        List<KeywordValueEnum> identities,
        KeywordValueEnum savingPeriod,
        List<KeywordValueEnum> coreBenefits,
        List<KeywordValueEnum> bankConditions
){

    public static ResolvedKeywords emptyKeywords() {
        return new ResolvedKeywords(List.of(), List.of(), null, List.of(), List.of());
    }

    // 대분류와 맞지 않는 저축기간은 고르지 않은 것으로 본다 (적합도 기간 축에서 빠지고, 단기는 1·3개월 상품을 모두 보여준다).
    public ResolvedKeywords withoutMismatchedPeriod(ProductCategoryEnum category) {
        if (savingPeriod == null || ProductCategoryEnum.fromSaveTrm(savingPeriod.toSaveTrm()) == category) {
            return this;
        }
        return new ResolvedKeywords(regions, identities, null, coreBenefits, bankConditions);
    }
}