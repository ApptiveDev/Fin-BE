package apptive.fin.search.service;

import apptive.fin.search.dto.EligibleProductOption;
import apptive.fin.search.entity.ProductProperty;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class BankMaxInterestPolicy {

    private BankMaxInterestPolicy() {
    }

    public static Double calculateThreshold(List<Double> rates) {
        if (rates == null || rates.isEmpty()) {
            return null;
        }

        List<Double> sortedDesc = rates.stream()
                .sorted(Comparator.reverseOrder())
                .toList();
        int cutoffIndex = (int) Math.ceil(sortedDesc.size() * 0.3) - 1;
        return sortedDesc.get(Math.max(cutoffIndex, 0));
    }

    // 후보 옵션 중 은행 옵션의 maxRate로 계산한다. 목록·상세·찜이 같이 쓴다.
    public static Double calculateThresholdOf(List<EligibleProductOption> options) {
        return calculateThreshold(options.stream()
                .filter(option -> option.product().isBank())
                .map(option -> option.property().getMaxRate())
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .toList());
    }

    public static boolean qualifies(ProductProperty property, Double threshold) {
        return threshold != null
                && property.getMaxRate() != null
                && property.getMaxRate().doubleValue() >= threshold;
    }

    public static boolean anyJoinableQualifies(List<ProductProperty> properties, Double threshold) {
        return properties.stream()
                .filter(ProductProperty::isJoinable)
                .anyMatch(property -> qualifies(property, threshold));
    }
}
