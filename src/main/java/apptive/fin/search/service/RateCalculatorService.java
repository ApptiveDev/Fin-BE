package apptive.fin.search.service;

import apptive.fin.search.ContributionType;
import apptive.fin.search.ProductType;
import apptive.fin.search.dto.ProductRateDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;

@Service
public class RateCalculatorService {

    private static final String ONTONG_SOURCE = "ONTONG";

    public ProductRateDto calculate(Product product, SearchRequestDto request) {
        if (product.getType() == ProductType.SUBSCRIPTION) {
            return subscriptionDto(product);
        }

        if (isGovernmentProduct(product)) {
            return calculateGovernmentProduct(product, request);
        }

        return calculateBankProduct(product);
    }

    private ProductRateDto subscriptionDto(Product product) {
        return ProductRateDto.builder()
                .productId(product.getId())
                .productPropertyId(null)
                .productName(product.getProductName())
                .providerName(null)
                .source(product.getSource().getCode())
                .rateComparable(false)
                .isSubscription(true)
                .subscriptionNote("청약: 금리 비교 대상 아님")
                .build();
    }

    private ProductRateDto calculateGovernmentProduct(Product product, SearchRequestDto request) {
        Optional<GovYieldScore> bestScore = product.getProperties().stream()
                .map(property -> new GovYieldScore(property, calculateGovernmentYield(property, request)))
                .filter(score -> score.yield() != null)
                .max(Comparator.comparingDouble(GovYieldScore::yield));

        if (bestScore.isEmpty()) {
            ProductProperty firstProperty = product.getProperties().stream().findFirst().orElse(null);
            return baseDto(product, firstProperty)
                    .rateComparable(false)
                    .isSubscription(false)
                    .build();
        }

        GovYieldScore score = bestScore.get();
        return baseDto(product, score.property())
                .baseRate(0.0)
                .achievableRate(score.yield())
                .rateComparable(true)
                .isSubscription(false)
                .build();
    }

    private ProductRateDto calculateBankProduct(Product product) {
        ProductProperty bestProperty = product.getProperties().stream()
                .max(Comparator.comparingDouble(this::effectiveRate))
                .orElse(null);

        return baseDto(product, bestProperty)
                .baseRate(baseRate(bestProperty))
                .achievableRate(effectiveRate(bestProperty))
                .rateComparable(true)
                .isSubscription(false)
                .build();
    }

    private ProductRateDto.ProductRateDtoBuilder baseDto(Product product, ProductProperty property) {
        return ProductRateDto.builder()
                .productId(product.getId())
                .productPropertyId(property != null ? property.getId() : null)
                .productName(product.getProductName())
                .providerName(providerName(property))
                .source(product.getSource().getCode());
    }

    private Double calculateGovernmentYield(ProductProperty property, SearchRequestDto request) {
        if (property == null || Boolean.TRUE.equals(property.getExcludeFromRateComparison())) {
            return null;
        }

        ContributionType contributionType = property.getGovContributionType();
        if (contributionType == null || contributionType == ContributionType.NONE) {
            return null;
        }

        Double years = contributionYears(property);
        if (years == null || years <= 0) {
            return null;
        }

        return switch (contributionType) {
            case RATIO -> ratioYield(property.getGovMatchingRatio(), years);
            case FIXED_AMOUNT -> fixedAmountYield(property.getGovMonthlyFixedContribution(), monthlySavingsGoal(request), years);
            case NONE -> null;
        };
    }

    private Double ratioYield(BigDecimal matchingRatio, double years) {
        if (matchingRatio == null) {
            return null;
        }

        return matchingRatio.doubleValue() / years * 100;
    }

    private Double fixedAmountYield(Long monthlyFixedContribution, Long monthlySavingsGoal, double years) {
        if (monthlyFixedContribution == null || monthlySavingsGoal == null || monthlySavingsGoal <= 0) {
            return null;
        }

        return ((double) monthlyFixedContribution / monthlySavingsGoal) / years * 100;
    }

    private Double contributionYears(ProductProperty property) {
        Integer periodMonths = property.getGovContributionPeriodMonths() != null
                ? property.getGovContributionPeriodMonths()
                : property.getSaveTrm();

        if (periodMonths == null || periodMonths <= 0) {
            return null;
        }

        return periodMonths / 12.0;
    }

    private Long monthlySavingsGoal(SearchRequestDto request) {
        return request.detailedOptions() != null
                ? request.detailedOptions().monthlySavingsGoal()
                : null;
    }

    private boolean isGovernmentProduct(Product product) {
        return product.getSource().getCode().equals(ONTONG_SOURCE);
    }

    private double baseRate(ProductProperty property) {
        return property != null && property.getBaseRate() != null
                ? property.getBaseRate().doubleValue()
                : 0.0;
    }

    private double effectiveRate(ProductProperty property) {
        if (property == null) {
            return 0.0;
        }
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

    private record GovYieldScore(ProductProperty property, Double yield) {
    }
}
