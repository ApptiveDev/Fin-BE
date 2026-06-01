package apptive.fin.search.service;

import apptive.fin.search.ExtractionConfidence;
import apptive.fin.search.KeywordValueEnum;
import apptive.fin.search.RequiredKeywordEffect;
import apptive.fin.search.dto.ResolvedKeywords;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductProperty;
import apptive.fin.search.entity.ProductRequiredKeyword;
import apptive.fin.search.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EligibilityFilterService {

    private final ProductRepository productRepository;
    private final ResolveKeywordService resolveKeywordService;

    @Transactional(readOnly = true)
    public List<Product> filterEligible(SearchRequestDto request){
        var detail = request.detailedOptions();
        // TODO : QueryDSL 도입
        if (detail == null) return List.of();

        ResolvedKeywords keywords = resolveKeywordService.resolveKeywords(request.options());

        Integer age = detail.birthdate() != null
                ? Period.between(detail.birthdate(), LocalDate.now()).getYears()
                : null;

        Long annualIncome = detail.annualIncome();
        Integer householdIncomePercent = detail.householdIncomePercent();
        Boolean incomeProofUnavailable = annualIncome != null && annualIncome == 0L;
        Boolean militaryAgeExtensionRequested = keywords.identities().contains(KeywordValueEnum.STATUS_MILITARY);

        Boolean isHomeless = detail.isHomeless();
        Boolean isHouseholder = detail.isHouseholder();

        Integer tenureMonths = new HashSet<>(keywords.identities())
                .contains(KeywordValueEnum.STATUS_UNEMPLOYED)
                ? Integer.valueOf(0)
                : detail.tenureMonths();


        Long monthlyDeposit = detail.monthlySavingsGoal();

        return productRepository.findEligibleProducts(
                age, annualIncome, householdIncomePercent, incomeProofUnavailable, militaryAgeExtensionRequested, isHomeless,
                isHouseholder,tenureMonths, monthlyDeposit
        ).stream()
                .filter(product -> hasEligibleIdentityProperty(product, keywords.identities()))
                .toList();

    }

    private boolean hasEligibleIdentityProperty(Product product, List<KeywordValueEnum> identities) {
        return product.getProperties().stream()
                .anyMatch(property -> isIdentityEligible(property, identities));
    }

    private boolean isIdentityEligible(ProductProperty property, List<KeywordValueEnum> identities) {
        List<ProductRequiredKeyword> identityConditions = property.getRequiredKeywords().stream()
                .filter(this::isHighConfidenceIdentityCondition)
                .toList();

        boolean excluded = identityConditions.stream()
                .filter(condition -> condition.getEffect() == RequiredKeywordEffect.EXCLUDE)
                .map(ProductRequiredKeyword::getKeywordCode)
                .anyMatch(identities::contains);

        if (excluded) {
            return false;
        }

        List<KeywordValueEnum> requiredIdentities = identityConditions.stream()
                .filter(condition -> condition.getEffect() == RequiredKeywordEffect.REQUIRE)
                .map(ProductRequiredKeyword::getKeywordCode)
                .toList();

        return requiredIdentities.isEmpty() || requiredIdentities.stream().anyMatch(identities::contains);
    }

    private boolean isHighConfidenceIdentityCondition(ProductRequiredKeyword condition) {
        KeywordValueEnum keyword = condition.getKeywordCode();
        return condition.getConfidence() == ExtractionConfidence.HIGH
                && keyword != null
                && keyword.name().startsWith("STATUS_");
    }
}
