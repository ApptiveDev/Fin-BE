package apptive.fin.search.service;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.global.error.BusinessException;
import apptive.fin.search.KeywordValueEnum;
import apptive.fin.search.SearchErrorCode;
import apptive.fin.search.dto.ProductMatchDto;
import apptive.fin.search.dto.ProductRateDto;
import apptive.fin.search.dto.ProductSearchResultDto;
import apptive.fin.search.dto.ResolvedKeywords;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.dto.TabAvailabilityDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductKeyword;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final EligibilityFilterService eligibilityFilterService;
    private final MatchScoreService matchScoreService;
    private final RateCalculatorService rateCalculatorService;
    private final ResolveKeywordService resolveKeywordService;

    public ProductSearchResultDto search(SearchRequestDto request) {
        return search(request, null);
    }

    public ProductSearchResultDto search(SearchRequestDto request, AuthUserDetails userDetails) {
        ResolvedKeywords resolvedKeywords = resolveKeywordService.resolveKeywords(request.options());
        validateKeywordSelected(resolvedKeywords);

        List<Product> eligible = eligibilityFilterService.filterEligible(request);
        if (!resolvedKeywords.regions().isEmpty()) {
            eligible = eligible.stream()
                    .filter(product -> isBankProduct(product) || hasMatchingRegion(product, resolvedKeywords.regions()))
                    .toList();
        }

        List<Product> govList = eligible.stream()
                .filter(p -> p.getSource().getCode().equals("ONTONG"))
                .toList();
        List<Product> bankList = eligible.stream()
                .filter(p -> p.getSource().getCode().equals("FSS"))
                .toList();

        List<ProductMatchDto> govRanked = govList.stream()
                .map(p -> matchScoreService.score(p, request, resolvedKeywords))
                .sorted(Comparator.comparingDouble(ProductMatchDto::totalScore).reversed())
                .toList();
        List<ProductMatchDto> bankRanked = bankList.stream()
                .map(p -> matchScoreService.score(p, request, resolvedKeywords))
                .sorted(Comparator.comparingDouble(ProductMatchDto::totalScore).reversed())
                .toList();

        boolean tabBEnabled = isTabBEnabled(request, userDetails);
        TabAvailabilityDto tabs = TabAvailabilityDto.builder()
                .tabAEnabled(true)
                .tabBEnabled(tabBEnabled)
                .tabBDisabledReason(tabBEnabled ? null : "로그인 후 상세 정보를 입력하면 금리순 정렬을 확인할 수 있어요.")
                .build();

        List<ProductRateDto> governmentRateRanked = tabBEnabled
                ? govList.stream()
                        .map(p -> rateCalculatorService.calculate(p, request))
                        .filter(r -> !r.isSubscription())
                        .sorted(Comparator.comparingDouble(ProductRateDto::achievableRate).reversed())
                        .toList()
                : List.of();

        List<ProductRateDto> bankRateRanked = tabBEnabled
                ? bankList.stream()
                        .map(p -> rateCalculatorService.calculate(p, request))
                        .sorted(Comparator.comparingDouble(ProductRateDto::achievableRate).reversed())
                        .toList()
                : List.of();

        List<ProductRateDto> subscriptions = tabBEnabled
                ? govList.stream()
                        .map(p -> rateCalculatorService.calculate(p, request))
                        .filter(ProductRateDto::isSubscription)
                        .toList()
                : List.of();

        return ProductSearchResultDto.builder()
                .tabs(tabs)
                .governmentRanked(govRanked)
                .bankRanked(bankRanked)
                .governmentRateRanked(governmentRateRanked)
                .bankRateRanked(bankRateRanked)
                .subscriptionProducts(subscriptions)
                .build();
    }

    private boolean isTabBEnabled(SearchRequestDto request, AuthUserDetails userDetails) {
        if (userDetails == null || request.detailedOptions() == null) {
            return false;
        }

        var detail = request.detailedOptions();
        return detail.birthdate() != null
                && detail.annualIncome() != null
                && detail.householdSize() != null
                && detail.householdIncomePercent() != null
                && detail.monthlySavingsGoal() != null
                && detail.selectedInterestRateOptions() != null;
    }

    private void validateKeywordSelected(ResolvedKeywords keywords) {
        boolean hasSelectedKeyword = !keywords.regions().isEmpty()
                || !keywords.identities().isEmpty()
                || keywords.savingPeriod() != null
                || !keywords.coreBenefits().isEmpty()
                || !keywords.bankConditions().isEmpty();

        if (!hasSelectedKeyword) {
            throw new BusinessException(SearchErrorCode.KEYWORD_REQUIRED);
        }
    }

    private boolean hasMatchingRegion(Product product, List<KeywordValueEnum> selectedRegions) {
        List<KeywordValueEnum> productRegions = product.getProperties().stream()
                .flatMap(property -> property.getKeywords().stream())
                .map(ProductKeyword::getKeywordCode)
                .filter(keyword -> keyword.name().startsWith("REGION_"))
                .toList();

        return productRegions.isEmpty()
                || selectedRegions.stream().anyMatch(productRegions::contains);
    }

    private boolean isBankProduct(Product product) {
        return product.getSource().getCode().equals("FSS");
    }
}
