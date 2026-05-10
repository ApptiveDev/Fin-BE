package apptive.fin.search.service;

import apptive.fin.search.KeywordValueEnum;
import apptive.fin.search.dto.ProductRateDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductKeyword;
import apptive.fin.search.entity.ProductProperty;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class RateCalculatorService {
    public ProductRateDto calculate(Product p, SearchRequestDto request) {
        ProductProperty subscriptionProperty = findSubscriptionProperty(p);
        if (subscriptionProperty != null) {
            return ProductRateDto.builder()
                    .productId(p.getId())
                    .productPropertyId(subscriptionProperty.getId())
                    .productName(p.getProductName())
                    .providerName(providerName(subscriptionProperty))
                    .source(p.getSource().getCode())
                    .isSubscription(true)
                    .subscriptionNote("청약: 금리 비교 대상 아님")
                    .build();
        }

        ProductProperty bestProperty = p.getProperties().stream()
                .max(Comparator.comparingDouble(this::effectiveRate))
                .orElse(null);

        boolean isGov = p.getSource().getCode().equals("ONTONG");
        double base = baseRate(bestProperty);
        double achievableRate = isGov
                ? base + contributionRate(bestProperty) + group1BankBonus(p)
                : (bestProperty != null ? effectiveRate(bestProperty) : base);

        return ProductRateDto.builder()
                .productId(p.getId())
                .productPropertyId(bestProperty != null ? bestProperty.getId() : null)
                .productName(p.getProductName())
                .providerName(providerName(bestProperty))
                .source(p.getSource().getCode())
                .baseRate(base)
                .achievableRate(achievableRate)
                .isSubscription(false)
                .build();
    }

    private ProductProperty findSubscriptionProperty(Product p) {
        return p.getProperties().stream()
                .filter(property -> hasKeyword(property, KeywordValueEnum.INTEREST_SAVINGS))
                .findFirst()
                .orElse(null);
    }

    private boolean hasKeyword(ProductProperty property, KeywordValueEnum keyword) {
        return property.getKeywords().stream()
                .map(ProductKeyword::getKeywordCode)
                .anyMatch(keyword::equals);
    }

    private boolean isGroup1Product(Product p) {
        return p.getProperties().stream()
                .flatMap(property -> property.getKeywords().stream())
                .map(ProductKeyword::getKeywordCode)
                .anyMatch(keyword -> keyword == KeywordValueEnum.STATUS_MILITARY
                        || keyword == KeywordValueEnum.STATUS_SME_WORKER);
    }

    private double group1BankBonus(Product p) {
        if (!isGroup1Product(p)) return 0.0;

        return p.getProperties().stream()
                .filter(property -> property.getMaxRate() != null && property.getBaseRate() != null)
                .mapToDouble(property -> property.getMaxRate().doubleValue() - property.getBaseRate().doubleValue())
                .max()
                .orElse(0.0);
    }

    private double contributionRate(ProductProperty property) {
        return property != null && property.getGovContributionRate() != null
                ? property.getGovContributionRate().doubleValue()
                : 0.0;
    }

    private double baseRate(ProductProperty property) {
        return property != null && property.getBaseRate() != null
                ? property.getBaseRate().doubleValue()
                : 0.0;
    }

    private double effectiveRate(ProductProperty property) {
        if (property.getMaxRate() != null) {
            return property.getMaxRate().doubleValue();
        }
        if (property.getBaseRate() != null) {
            return property.getBaseRate().doubleValue();
        }
        return 0.0;
    }

    private String providerName(ProductProperty property) {
        return property != null && property.getProvider() != null
                ? property.getProvider().getName()
                : null;
    }
}
