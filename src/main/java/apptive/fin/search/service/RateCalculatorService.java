package apptive.fin.search.service;

import apptive.fin.search.KeywordValueEnum;
import apptive.fin.search.dto.ProductRateDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductProperty;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class RateCalculatorService {
    public ProductRateDto calculate(Product p, SearchRequestDto request) {
        ProductProperty bestProperty = p.getProperties().stream()
                .max(Comparator.comparingDouble(this::effectiveRate))
                .orElse(null);

        boolean hasRateOption = p.getProperties().stream()
                .anyMatch(property -> property.getIntrRate() != null || property.getIntrRate2() != null);
        if (hasRateOption) {
            return ProductRateDto.builder()
                    .productId(p.getId())
                    .productName(p.getProductName())
                    .source(p.getSource().getCode())
                    .baseRate(baseRate(bestProperty))
                    .achievableRate(bestProperty != null ? effectiveRate(bestProperty) : 0.0)
                    .isSubscription(false)
                    .build();
        }

        boolean isSubscription = p.getKeywords().stream()
                .anyMatch(k -> k.getKeywordCode() == KeywordValueEnum.INTEREST_SAVINGS);

        if (isSubscription) {
            return ProductRateDto.builder()
                    .productId(p.getId())
                    .productName(p.getProductName())
                    .source(p.getSource().getCode())
                    .isSubscription(true)
                    .subscriptionNote("Subscription product: excluded from rate comparison")
                    .build();
        }

        double baseRate = baseRate(bestProperty);
        double maxRate = bestProperty != null ? effectiveRate(bestProperty) : baseRate;

        return ProductRateDto.builder()
                .productId(p.getId())
                .productName(p.getProductName())
                .source(p.getSource().getCode())
                .baseRate(baseRate)
                .achievableRate(maxRate)
                .isSubscription(false)
                .build();
    }

    private double baseRate(ProductProperty property) {
        return property != null && property.getBaseRate() != null
                ? property.getBaseRate().doubleValue()
                : 0.0;
    }

    private double effectiveRate(ProductProperty property) {
        if (property.getIntrRate2() != null) {
            return property.getIntrRate2().doubleValue();
        }
        if (property.getIntrRate() != null) {
            return property.getIntrRate().doubleValue();
        }
        if (property.getMaxRate() != null) {
            return property.getMaxRate().doubleValue();
        }
        if (property.getBaseRate() != null) {
            return property.getBaseRate().doubleValue();
        }
        return 0.0;
    }
}
