package apptive.fin.search;

import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.ProductMatchDto;
import apptive.fin.search.dto.ResolvedKeywords;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.entity.ProductKeyword;
import apptive.fin.search.entity.ProductProperty;
import apptive.fin.search.entity.ProductSource;
import apptive.fin.search.entity.Provider;
import apptive.fin.search.service.MatchScoreService;
import apptive.fin.search.service.ResolveKeywordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchScoreServiceTest {

    @Mock
    private ResolveKeywordService resolveKeywordService;

    @Test
    void 월저축목표가_null이어도_예외없이_저축액점수를_제외한다() {
        MatchScoreService matchScoreService = new MatchScoreService(resolveKeywordService);
        Product product = new Product();
        ProductSource source = new ProductSource();
        Provider provider = new Provider();
        ProductProperty property = new ProductProperty();

        ReflectionTestUtils.setField(source, "code", "FSS");
        ReflectionTestUtils.setField(provider, "name", "테스트은행");
        ReflectionTestUtils.setField(property, "id", 10L);
        ReflectionTestUtils.setField(property, "provider", provider);
        ReflectionTestUtils.setField(property, "maxMonthlyLimit", 500_000L);
        ReflectionTestUtils.setField(property, "keywords", new ArrayList<>());
        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "productName", "청년우대적금");
        ReflectionTestUtils.setField(product, "source", source);
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(property)));

        SearchRequestDto request = new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(
                        null, null, null, null, null,
                        null, null, null, null, null, List.of()
                )
        );
        when(resolveKeywordService.resolveKeywords(request.options()))
                .thenReturn(new ResolvedKeywords(List.of(), List.of(), null, List.of(), List.of()));

        // List → 단일 반환으로 변경
        ProductMatchDto result = matchScoreService.score(product, request);

        assertThat(result.depositScore()).isZero();
        assertThat(result.productPropertyId()).isEqualTo(10L);
        assertThat(result.providerName()).isEqualTo("테스트은행");
    }

    @Test
    void 옵션이_여러개이면_최고점수_옵션_하나만_반환한다() {
        MatchScoreService matchScoreService = new MatchScoreService(resolveKeywordService);
        Product product = new Product();
        ProductSource source = new ProductSource();

        // maxMonthlyLimit이 더 큰 쪽이 depositScore 높음
        ProductProperty firstProperty  = createProperty(10L, "테스트은행A", 300_000L);
        ProductProperty secondProperty = createProperty(11L, "테스트은행B", 500_000L);

        ReflectionTestUtils.setField(source, "code", "FSS");
        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "productName", "청년우대적금");
        ReflectionTestUtils.setField(product, "source", source);
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(firstProperty, secondProperty)));

        SearchRequestDto request = new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(
                        null, null, null, null, null,
                        null, null, null, 400_000L, null, List.of()
                )
        );
        when(resolveKeywordService.resolveKeywords(request.options()))
                .thenReturn(new ResolvedKeywords(List.of(), List.of(), null, List.of(), List.of()));

        // List → 단일 반환으로 변경
        ProductMatchDto result = matchScoreService.score(product, request);

        // 상품 하나로 합쳐져서 단일 반환
        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.productName()).isEqualTo("청년우대적금");

        // maxMonthlyLimit이 더 큰 secondProperty(11L)가 최고 점수
        // 희망 40만 > 한도 30만(10L) → depositScore 감점
        // 희망 40만 ≤ 한도 50만(11L) → depositScore 만점
        assertThat(result.productPropertyId()).isEqualTo(11L);
        assertThat(result.providerName()).isEqualTo("테스트은행B");
    }

    private ProductProperty createProperty(Long id, String providerName, Long maxMonthlyLimit) {
        Provider provider = new Provider();
        ProductProperty property = new ProductProperty();

        ReflectionTestUtils.setField(provider, "name", providerName);
        ReflectionTestUtils.setField(property, "id", id);
        ReflectionTestUtils.setField(property, "provider", provider);
        ReflectionTestUtils.setField(property, "maxMonthlyLimit", maxMonthlyLimit);
        ReflectionTestUtils.setField(property, "keywords", new ArrayList<>());
        return property;
    }

    @Test
    void identity_not_selected_reallocates_identity_weight_to_active_bank_scores() {
        MatchScoreService matchScoreService = new MatchScoreService(resolveKeywordService);
        Product product = createProduct("FSS", createProperty(
                10L,
                "test-bank",
                500_000L,
                12,
                KeywordValueEnum.BENEFIT_EASY_CONDITION,
                KeywordValueEnum.BANK_CARD_USAGE
        ));

        ProductMatchDto result = matchScoreService.score(
                product,
                createRequest(300_000L),
                new ResolvedKeywords(
                        List.of(),
                        List.of(),
                        KeywordValueEnum.TERM_AROUND_1_YEAR,
                        List.of(KeywordValueEnum.BENEFIT_EASY_CONDITION),
                        List.of(KeywordValueEnum.BANK_CARD_USAGE)
                )
        );

        assertThat(result.identityScore()).isZero();
        assertThat(result.totalScore()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void bank_unavailable_benefits_are_excluded_and_reallocated() {
        MatchScoreService matchScoreService = new MatchScoreService(resolveKeywordService);
        Product product = createProduct("FSS", createProperty(
                10L,
                "test-bank",
                500_000L,
                12,
                KeywordValueEnum.STATUS_MILITARY,
                KeywordValueEnum.BANK_CARD_USAGE
        ));

        ProductMatchDto result = matchScoreService.score(
                product,
                createRequest(300_000L),
                new ResolvedKeywords(
                        List.of(),
                        List.of(KeywordValueEnum.STATUS_MILITARY),
                        KeywordValueEnum.TERM_AROUND_1_YEAR,
                        List.of(KeywordValueEnum.BENEFIT_GOV_SUBSIDY),
                        List.of(KeywordValueEnum.BANK_CARD_USAGE)
                )
        );

        assertThat(result.benefitScore()).isZero();
        assertThat(result.totalScore()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void gov_subscription_excludes_bank_condition_and_reallocates_weight() {
        MatchScoreService matchScoreService = new MatchScoreService(resolveKeywordService);
        Product product = createProduct("ONTONG", createProperty(
                10L,
                "policy-provider",
                500_000L,
                12,
                KeywordValueEnum.BENEFIT_GOV_SUBSIDY,
                KeywordValueEnum.STATUS_MILITARY,
                KeywordValueEnum.BANK_CARD_USAGE,
                KeywordValueEnum.INTEREST_SAVINGS
        ));

        ProductMatchDto result = matchScoreService.score(
                product,
                createRequest(300_000L),
                new ResolvedKeywords(
                        List.of(),
                        List.of(KeywordValueEnum.STATUS_MILITARY),
                        KeywordValueEnum.TERM_AROUND_1_YEAR,
                        List.of(KeywordValueEnum.BENEFIT_GOV_SUBSIDY),
                        List.of(KeywordValueEnum.BANK_CARD_USAGE)
                )
        );

        assertThat(result.bankCondScore()).isZero();
        assertThat(result.totalScore()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void gov_product_with_bank_provider_keeps_bank_condition_weight() {
        MatchScoreService matchScoreService = new MatchScoreService(resolveKeywordService);
        ProductProperty property = createProperty(
                10L,
                "KB",
                500_000L,
                12,
                KeywordValueEnum.BENEFIT_GOV_SUBSIDY,
                KeywordValueEnum.STATUS_MILITARY,
                KeywordValueEnum.BANK_CARD_USAGE,
                KeywordValueEnum.INTEREST_SAVINGS
        );
        setProviderCode(property, "KB");
        Product product = createProduct("ONTONG", property);

        ProductMatchDto result = matchScoreService.score(
                product,
                createRequest(300_000L),
                new ResolvedKeywords(
                        List.of(),
                        List.of(KeywordValueEnum.STATUS_MILITARY),
                        KeywordValueEnum.TERM_AROUND_1_YEAR,
                        List.of(KeywordValueEnum.BENEFIT_GOV_SUBSIDY),
                        List.of(KeywordValueEnum.BANK_CARD_USAGE)
                )
        );

        assertThat(result.bankCondScore()).isCloseTo(8.0, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.totalScore()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.001));
    }

    private Product createProduct(String sourceCode, ProductProperty property) {
        ProductSource source = new ProductSource();
        Product product = new Product();

        ReflectionTestUtils.setField(source, "code", sourceCode);
        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "productName", "test-product");
        ReflectionTestUtils.setField(product, "source", source);
        ReflectionTestUtils.setField(product, "properties", new ArrayList<>(List.of(property)));
        return product;
    }

    private ProductProperty createProperty(
            Long id,
            String providerName,
            Long maxMonthlyLimit,
            Integer saveTrm,
            KeywordValueEnum... keywords
    ) {
        ProductProperty property = createProperty(id, providerName, maxMonthlyLimit);
        ReflectionTestUtils.setField(property, "saveTrm", saveTrm);
        for (KeywordValueEnum keyword : keywords) {
            addKeyword(property, keyword);
        }
        return property;
    }

    private void addKeyword(ProductProperty property, KeywordValueEnum keywordValue) {
        ProductKeyword keyword = new ProductKeyword();
        ReflectionTestUtils.setField(keyword, "keywordCode", keywordValue);
        List<ProductKeyword> keywords = new ArrayList<>(
                (List<ProductKeyword>) ReflectionTestUtils.getField(property, "keywords")
        );
        keywords.add(keyword);
        ReflectionTestUtils.setField(property, "keywords", keywords);
    }

    private void setProviderCode(ProductProperty property, String code) {
        Provider provider = property.getProvider();
        ReflectionTestUtils.setField(provider, "code", code);
    }

    private SearchRequestDto createRequest(Long monthlySavingsGoal) {
        return new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(
                        null, null, null, null, null,
                        null, null, null, monthlySavingsGoal, null, List.of()
                )
        );
    }
}
