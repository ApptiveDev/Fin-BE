package apptive.fin.calendar.service;

import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductProperty;

import java.util.List;
import java.util.Objects;

/**
 * 1계층 정책상품을 캘린더 카드에 보일 조건. 가입 가능한 옵션만 본다.
 * 옵션(일반형·우대형 등)마다 나이가 다르면 가장 넓은 범위를 쓰고, 한 옵션이라도 제한이 없으면 제한 없음으로 본다.
 * 군 복무 연장 나이는 보지 않는다(plan-vs-implementation 참고).
 *
 * @param regions     옵션 키워드의 거주지역 키(REGION_*). 없으면 전국
 * @param institution 첫 번째 가입 가능 옵션의 제공기관
 */
record ProductConditions(Integer minAge, Integer maxAge, List<String> regions, String institution) {

    private static final String REGION_CODE_PREFIX = "REGION_";

    static ProductConditions of(Product product) {
        List<ProductProperty> joinable = product.getProperties().stream()
                .filter(ProductProperty::isJoinable)
                .toList();
        return new ProductConditions(minAgeOf(joinable), maxAgeOf(joinable), regionsOf(joinable), institutionOf(joinable));
    }

    private static Integer minAgeOf(List<ProductProperty> properties) {
        if (properties.isEmpty() || properties.stream().anyMatch(property -> property.getMinAge() == null)) {
            return null;
        }
        return properties.stream().mapToInt(ProductProperty::getMinAge).min().getAsInt();
    }

    private static Integer maxAgeOf(List<ProductProperty> properties) {
        if (properties.isEmpty() || properties.stream().anyMatch(property -> property.getMaxAge() == null)) {
            return null;
        }
        return properties.stream().mapToInt(ProductProperty::getMaxAge).max().getAsInt();
    }

    private static List<String> regionsOf(List<ProductProperty> properties) {
        return properties.stream()
                .flatMap(property -> property.keywordCodes().stream())
                .map(Enum::name)
                .filter(code -> code.startsWith(REGION_CODE_PREFIX))
                .distinct()
                .sorted()
                .toList();
    }

    private static String institutionOf(List<ProductProperty> properties) {
        return properties.stream()
                .map(ProductProperty::providerName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
