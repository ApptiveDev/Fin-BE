package apptive.fin.search;

import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.ProductRateDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductProperty;
import apptive.fin.search.entity.ProductSource;
import apptive.fin.search.entity.Provider;
import apptive.fin.search.service.RateCalculatorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateCalculatorServiceTest {

    private final RateCalculatorService rateCalculatorService = new RateCalculatorService();

    @Test
    void 옵션이_여러개이면_최고금리_옵션_하나만_반환한다() {
        Product product = createProduct("BANK001", "청년우대적금", "FSS");
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(
                createProperty(10L, "테스트은행", "3.80", "4.50"),
                createProperty(11L, "테스트은행", "3.50", "4.20")
        )));

        ProductRateDto result = rateCalculatorService.calculate(product, createRequest());

        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.productName()).isEqualTo("청년우대적금");
        assertThat(result.productPropertyId()).isEqualTo(10L);
        assertThat(result.baseRate()).isEqualTo(3.8);
        assertThat(result.achievableRate()).isEqualTo(4.5);
        assertThat(result.rateComparable()).isTrue();
        assertThat(result.isSubscription()).isFalse();
    }

    @Test
    void 최고금리가_없으면_기본금리를_사용한다() {
        Product product = createProduct("BANK002", "기본금리상품", "FSS");
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(
                createProperty(10L, "테스트은행", "2.75", null)
        )));

        ProductRateDto result = rateCalculatorService.calculate(product, createRequest());

        assertThat(result.baseRate()).isEqualTo(2.75);
        assertThat(result.achievableRate()).isEqualTo(2.75);
        assertThat(result.rateComparable()).isTrue();
    }

    @Test
    void 청약상품은_금리비교_대상에서_제외하고_청약상품으로_반환한다() {
        Product product = createProduct("GOV001", "청약상품", "ONTONG");
        ProductProperty property = createProperty(10L, "테스트은행", null, null);
        ReflectionTestUtils.setField(product, "type", ProductType.SUBSCRIPTION);
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(property)));

        ProductRateDto result = rateCalculatorService.calculate(product, createRequest());

        assertThat(result.isSubscription()).isTrue();
        assertThat(result.rateComparable()).isFalse();
        assertThat(result.subscriptionNote()).isEqualTo("청약: 금리 비교 대상 아님");
    }

    @Test
    void 정부_정률상품은_기여금_환산수익률로_계산한다() {
        Product product = createProduct("GOV002", "정부정률상품", "ONTONG");
        ProductProperty property = createProperty(10L, "정책기관", "4.00", "4.50");
        ReflectionTestUtils.setField(property, "govContributionType", ContributionType.RATIO);
        ReflectionTestUtils.setField(property, "govMatchingRatio", new BigDecimal("1.0000"));
        ReflectionTestUtils.setField(property, "govContributionPeriodMonths", 24);
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(property)));

        ProductRateDto result = rateCalculatorService.calculate(product, createRequest());

        assertThat(result.baseRate()).isZero();
        assertThat(result.achievableRate()).isEqualTo(50.0);
        assertThat(result.rateComparable()).isTrue();
        assertThat(result.isSubscription()).isFalse();
    }

    @Test
    void 정부_정액상품은_월납입액_기준_기여금_환산수익률로_계산한다() {
        Product product = createProduct("GOV003", "정부정액상품", "ONTONG");
        ProductProperty property = createProperty(10L, "정책기관", null, null);
        ReflectionTestUtils.setField(property, "govContributionType", ContributionType.FIXED_AMOUNT);
        ReflectionTestUtils.setField(property, "govMonthlyFixedContribution", 300_000L);
        ReflectionTestUtils.setField(property, "govContributionPeriodMonths", 36);
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(property)));

        ProductRateDto result = rateCalculatorService.calculate(product, createRequest(100_000L));

        assertThat(result.achievableRate()).isEqualTo(100.0);
        assertThat(result.rateComparable()).isTrue();
        assertThat(result.isSubscription()).isFalse();
    }

    @Test
    void 정부_정액상품에_월납입액이_없으면_금리비교에서_제외한다() {
        Product product = createProduct("GOV004", "계산불가정액상품", "ONTONG");
        ProductProperty property = createProperty(10L, "정책기관", null, null);
        ReflectionTestUtils.setField(property, "govContributionType", ContributionType.FIXED_AMOUNT);
        ReflectionTestUtils.setField(property, "govMonthlyFixedContribution", 300_000L);
        ReflectionTestUtils.setField(property, "govContributionPeriodMonths", 36);
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(property)));

        ProductRateDto result = rateCalculatorService.calculate(product, createRequest());

        assertThat(result.rateComparable()).isFalse();
        assertThat(result.achievableRate()).isZero();
        assertThat(result.isSubscription()).isFalse();
    }

    @Test
    void 비교제외_정부상품은_금리비교에서_제외한다() {
        Product product = createProduct("GOV005", "비교제외상품", "ONTONG");
        ProductProperty property = createProperty(10L, "정책기관", null, null);
        ReflectionTestUtils.setField(property, "excludeFromRateComparison", true);
        ReflectionTestUtils.setField(property, "govContributionType", ContributionType.RATIO);
        ReflectionTestUtils.setField(property, "govMatchingRatio", new BigDecimal("1.0000"));
        ReflectionTestUtils.setField(property, "govContributionPeriodMonths", 24);
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(property)));

        ProductRateDto result = rateCalculatorService.calculate(product, createRequest());

        assertThat(result.rateComparable()).isFalse();
        assertThat(result.isSubscription()).isFalse();
    }

    private Product createProduct(String code, String name, String sourceCode) {
        ProductSource source = new ProductSource();
        ReflectionTestUtils.setField(source, "code", sourceCode);

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "productCode", code);
        ReflectionTestUtils.setField(product, "productName", name);
        ReflectionTestUtils.setField(product, "source", source);
        ReflectionTestUtils.setField(product, "type", ProductType.SAVING);
        return product;
    }

    private ProductProperty createProperty(Long id, String providerName, String baseRate, String maxRate) {
        ProductProperty property = new ProductProperty();
        Provider provider = new Provider();
        ReflectionTestUtils.setField(provider, "name", providerName);
        ReflectionTestUtils.setField(property, "id", id);
        ReflectionTestUtils.setField(property, "provider", provider);
        ReflectionTestUtils.setField(property, "keywords", new ArrayList<>());
        if (baseRate != null) {
            ReflectionTestUtils.setField(property, "baseRate", new BigDecimal(baseRate));
        }
        if (maxRate != null) {
            ReflectionTestUtils.setField(property, "maxRate", new BigDecimal(maxRate));
        }
        return property;
    }

    private SearchRequestDto createRequest() {
        return createRequest(null);
    }

    private SearchRequestDto createRequest(Long monthlySavingsGoal) {
        return new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(null, null, null, null, null, null, null, null, monthlySavingsGoal, null, List.of())
        );
    }
}
