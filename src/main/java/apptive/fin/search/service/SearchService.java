package apptive.fin.search.service;

import apptive.fin.search.KeywordValueEnum;
import apptive.fin.search.dto.*;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductKeyword;
import apptive.fin.search.entity.ProductProperty;
import apptive.fin.search.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final EligibilityFilterService eligibilityFilterService;
    private final MatchScoreService matchScoreService;
    private final RateCalculatorService rateCalculatorService;
    private final ResolveKeywordService resolveKeywordService;
    private final ProductRepository productRepository;

    public ProductSearchResultDto search(SearchRequestDto request) {
        ResolvedKeywords resolvedKeywords = resolveKeywordService.resolveKeywords(request.options());

        List<Product> eligible = eligibilityFilterService.filterEligible(request);
        if (!resolvedKeywords.regions().isEmpty()) {
            eligible = eligible.stream()
                    .filter(product -> hasMatchingRegion(product, resolvedKeywords.regions()))
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

        List<ProductRateDto> allRated = Stream.concat(govList.stream(), bankList.stream())
                .map(p -> rateCalculatorService.calculate(p, request))
                .toList();

        List<ProductRateDto> rateRanked = allRated.stream()
                .filter(r -> !r.isSubscription())
                .sorted(Comparator.comparingDouble(ProductRateDto::achievableRate).reversed())
                .toList();

        List<ProductRateDto> subscriptions = allRated.stream()
                .filter(ProductRateDto::isSubscription)
                .toList();

        return ProductSearchResultDto.builder()
                .governmentRanked(govRanked)
                .bankRanked(bankRanked)
                .rateRanked(rateRanked)
                .subscriptionProducts(subscriptions)
                .build();
    }

    public List<ProductNameSearchDto> searchByName(String searchInput){
        return productRepository.findByProductNameContaining(searchInput)
                .stream()
                .map(p -> {
                    ProductProperty bestProperty = p.getProperties().stream()
                            .max(Comparator.comparingDouble(pp ->
                                    pp.getMaxRate() != null ? pp.getMaxRate().doubleValue(): 0.0 ))
                            .orElse(null);

                    return ProductNameSearchDto.builder()
                            .productId(p.getId())
                            .productName(p.getProductName())
                            .source(p.getSource().getCode())
                            .providerName(bestProperty != null && bestProperty.getProvider() != null
                                    ? bestProperty.getProvider().getName() : null)
                            .baseRate(bestProperty != null && bestProperty.getBaseRate() != null
                                    ? bestProperty.getBaseRate().doubleValue() : null)
                            .maxRate(bestProperty != null && bestProperty.getMaxRate() != null
                                    ? bestProperty.getMaxRate().doubleValue() : null)
                            .build();
                })
                .toList();
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
}
