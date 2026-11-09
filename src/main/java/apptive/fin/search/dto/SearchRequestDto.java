package apptive.fin.search.dto;

import apptive.fin.search.enums.ProductCategoryEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record SearchRequestDto(
        @NotNull List<@Valid OptionRequestDto> options,
        @NotNull DetailedOptionsDto detailedOptions,
        ProductCategoryEnum category  // 대분류(응답의 category와 같은 값). 결과 화면에서 전환할 때만 보낸다. 없으면 저축기간으로 정한다.
) {
    public SearchRequestDto(List<OptionRequestDto> options, DetailedOptionsDto detailedOptions) {
        this(options, detailedOptions, null);
    }

    public ProductCategoryEnum category(ResolvedKeywords keywords) {
        if (category != null) {
            return category;
        }
        return ProductCategoryEnum.fromSaveTrm(getSaveTrm(keywords));
    }

    // 상세옵션 널세이프 위임 접근자. detailedOptions가 없으면 null.
    public Integer age(LocalDate today) {
        return detailedOptions != null ? detailedOptions.age(today) : null;
    }

    public Integer age() {
        return detailedOptions != null ? detailedOptions.age() : null;
    }

    public Long monthlySavingsGoal() {
        return detailedOptions != null ? detailedOptions.monthlySavingsGoal() : null;
    }

    public List<String> neverUsedBanks() {
        return detailedOptions != null ? detailedOptions.neverUsedBanks() : null;
    }

    public List<String> maturedSavingBanks() {
        return detailedOptions != null ? detailedOptions.maturedSavingBanks() : null;
    }

    public boolean hasTransactionHistory() {
        return detailedOptions != null
                && detailedOptions.neverUsedBanks() != null
                && detailedOptions.maturedSavingBanks() != null;
    }

    // === 신규 필드 접근자 (PRD 개정) ===

    public Long depositAmount() {
        return detailedOptions != null ? detailedOptions.depositAmount() : null;
    }

    /**
     * 저축기간 조회 (키워드에서만 추출).
     * TERM_*_MONTH 키워드가 {1,3,6,12,24,36}만 허용하므로 별도 검증 불필요.
     */
    public Integer getSaveTrm(ResolvedKeywords keywords) {
        if (keywords != null && keywords.savingPeriod() != null) {
            return keywords.savingPeriod().toSaveTrm();
        }
        return null;
    }

    // 적금 계산용 월 납입액 (상품 기간 기준)
    public Long monthlyDepositFor(ResolvedKeywords keywords, Integer productSaveTrm) {
        if (detailedOptions == null) return null;
        return detailedOptions.monthlyDepositFor(category(keywords), productSaveTrm);
    }

    // 예금 계산용 예치 원금 (상품 기간 기준)
    public Long depositPrincipalFor(ResolvedKeywords keywords, Integer productSaveTrm) {
        if (detailedOptions == null) return null;
        return detailedOptions.depositPrincipalFor(category(keywords), productSaveTrm);
    }
}
