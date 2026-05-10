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
        // 그룹3 - 청약 상품(금리 비교 대상 아님)
        if (isSubscriptionProduct(p)) {
            return ProductRateDto.builder()
                    .productId(p.getId())
                    .productName(p.getProductName())
                    .source(p.getSource().getCode())
                    .isSubscription(true)
                    .subscriptionNote("청약: 금리 비교 대상 아님")
                    .build();
        }
        ProductProperty bestProperty = p.getProperties().stream()
                .max(Comparator.comparingDouble(this::effectiveRate))
                .orElse(null);

        boolean isGov = p.getSource().getCode().equals("ONTONG");

        // 그룹1 - 기본금리 + 기여금 환산 + 최급 은행 최고 우대 금리
        // 그룹2 - 기본금리 + 기여금 환산
        if (isGov) {
            double base = baseRate(bestProperty);
            double contributionRate = calcContributionRate(p, request);
            double bankBonus = isGroup1Product(p) ? getMaxBankBonusRate(p) : 0.0;

            return ProductRateDto.builder()
                    .productId(p.getId())
                    .productName(p.getProductName())
                    .source(p.getSource().getCode())
                    .baseRate(base)
                    .achievableRate(base + contributionRate + bankBonus)
                    .isSubscription(false)
                    .build();

        }

        // 시중은행 - 기본금리 + 달성 가능 우대금리
        double base = baseRate(bestProperty);
        double achievableRate = bestProperty != null ? effectiveRate(bestProperty) : base;

        return ProductRateDto.builder()
                .productId(p.getId())
                .productName(p.getProductName())
                .source(p.getSource().getCode())
                .baseRate(base)
                .achievableRate(achievableRate)
                .isSubscription(false)
                .build();
    }

    // 그룹3 판별 - 청약 상품
    private boolean isSubscriptionProduct(Product p) {
        return p.getKeywords().stream()
                .anyMatch(k -> k.getKeywordCode() == KeywordValueEnum.INTEREST_SAVINGS);
    }

    // 그룹1 판별 - 장병내일준비적금, 청년미래적금, 중소기업재직자 우대저축공제
    private boolean isGroup1Product(Product p) {
        return p.getKeywords().stream()
                .anyMatch(k -> k.getKeywordCode() == KeywordValueEnum.STATUS_MILITARY
                || k.getKeywordCode() == KeywordValueEnum.STATUS_SME_WORKER);
    }

    // 기여금 환산 금리
    private double calcContributionRate(Product p, SearchRequestDto request) {
        // TODO : 기여금 환산 로직 추후 구현
        return 0.0;
    }

    // 그룹1 전용 - 취급 은행 최고 우대금리
    private double getMaxBankBonusRate(Product p) {
        return p.getProperties().stream()
                .filter(pp -> pp.getMaxRate() != null && pp.getBaseRate() != null)
                .mapToDouble(pp -> pp.getMaxRate().doubleValue() - pp.getBaseRate().doubleValue())
                .max()
                .orElse(0.0);
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
}
