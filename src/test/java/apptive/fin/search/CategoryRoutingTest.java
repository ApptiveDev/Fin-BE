package apptive.fin.search;

import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.ResolvedKeywords;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.enums.KeywordValueEnum;
import apptive.fin.search.enums.ProductCategoryEnum;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대분류 라우팅 테스트.
 * 저축기간 키워드(TERM_*_MONTH)에 따라 대분류가 결정되는지 검증.
 */
class CategoryRoutingTest {

    @Test
    void saveTrm_1개월이면_단기예치_대분류다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(1)).isEqualTo(ProductCategoryEnum.SHORT_TERM);
    }

    @Test
    void saveTrm_3개월이면_단기예치_대분류다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(3)).isEqualTo(ProductCategoryEnum.SHORT_TERM);
    }

    @Test
    void saveTrm_6개월이면_목돈만들기_대분류다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(6)).isEqualTo(ProductCategoryEnum.LONG_TERM);
    }

    @Test
    void saveTrm_12개월이면_목돈만들기_대분류다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(12)).isEqualTo(ProductCategoryEnum.LONG_TERM);
    }

    @Test
    void saveTrm_null이면_기본값_목돈만들기다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(null)).isEqualTo(ProductCategoryEnum.LONG_TERM);
    }

    @Test
    void TERM_1_MONTH_키워드는_단기예치다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(KeywordValueEnum.TERM_1_MONTH.toSaveTrm()))
                .isEqualTo(ProductCategoryEnum.SHORT_TERM);
    }

    @Test
    void TERM_3_MONTH_키워드는_단기예치다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(KeywordValueEnum.TERM_3_MONTH.toSaveTrm()))
                .isEqualTo(ProductCategoryEnum.SHORT_TERM);
    }

    @Test
    void TERM_12_MONTH_키워드는_목돈만들기다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(KeywordValueEnum.TERM_12_MONTH.toSaveTrm()))
                .isEqualTo(ProductCategoryEnum.LONG_TERM);
    }

    @Test
    void 레거시_TERM_AROUND_1_YEAR_키워드는_목돈만들기다() {
        assertThat(ProductCategoryEnum.fromSaveTrm(KeywordValueEnum.TERM_AROUND_1_YEAR.toSaveTrm()))
                .isEqualTo(ProductCategoryEnum.LONG_TERM);
    }

    @Test
    void 단기예치_월납입액은_예치액을_고른_기간으로_나눈다() {
        DetailedOptionsDto dto = amounts(null, 3000000L);  // 예치액 300만

        // 고른 기간 3개월: 300만 / 3 = 100만
        assertThat(dto.monthlyDepositFor(ProductCategoryEnum.SHORT_TERM, 3, 3)).isEqualTo(1000000L);
    }

    @Test
    void 단기예치_월납입액은_고른_기간이_없으면_상품기간으로_나눈다() {
        DetailedOptionsDto dto = amounts(null, 3000000L);

        assertThat(dto.monthlyDepositFor(ProductCategoryEnum.SHORT_TERM, null, 3)).isEqualTo(1000000L);
    }

    @Test
    void 목돈만들기_예치원금은_월저축액에_고른_기간을_곱한다() {
        DetailedOptionsDto dto = amounts(500000L, null);  // 월저축액 50만

        // 고른 기간 12개월, 상품 기간 36개월: 50만 × 12 = 600만 (공통 기준원금)
        assertThat(dto.depositPrincipalFor(ProductCategoryEnum.LONG_TERM, 12, 36)).isEqualTo(6000000L);
    }

    @Test
    void 목돈만들기_예치원금은_고른_기간이_없으면_상품기간을_곱한다() {
        DetailedOptionsDto dto = amounts(500000L, null);

        assertThat(dto.depositPrincipalFor(ProductCategoryEnum.LONG_TERM, null, 36)).isEqualTo(18000000L);
    }

    @Test
    void 목돈만들기_예치원금은_고른_기간이_대분류와_다르면_상품기간을_곱한다() {
        DetailedOptionsDto dto = amounts(500000L, null);

        // 3개월을 고르고 목돈으로 전환: 3개월은 목돈 기간이 아니라 쓰지 않는다
        assertThat(dto.depositPrincipalFor(ProductCategoryEnum.LONG_TERM, 3, 12)).isEqualTo(6000000L);
    }

    @Test
    void 목돈만들기_월납입액은_예치액이_있어도_월저축액이다() {
        DetailedOptionsDto dto = amounts(500000L, 3000000L);

        assertThat(dto.monthlyDepositFor(ProductCategoryEnum.LONG_TERM, 12, 3)).isEqualTo(500000L);
    }

    @Test
    void 단기예치_예치원금은_상품기간이_길어도_예치액이다() {
        DetailedOptionsDto dto = amounts(500000L, 3000000L);

        assertThat(dto.depositPrincipalFor(ProductCategoryEnum.SHORT_TERM, null, 12)).isEqualTo(3000000L);
    }

    @Test
    void 요청의_예치원금은_키워드의_저축기간을_고른_기간으로_쓴다() {
        SearchRequestDto request = new SearchRequestDto(List.of(), amounts(500000L, null));
        ResolvedKeywords keywords = new ResolvedKeywords(
                List.of(), List.of(), KeywordValueEnum.TERM_12_MONTH, List.of(), List.of());

        assertThat(request.depositPrincipalFor(keywords, 36)).isEqualTo(6000000L);
    }

    private DetailedOptionsDto amounts(Long monthlySavingsGoal, Long depositAmount) {
        return new DetailedOptionsDto(
                null, null, null, null, null,
                null, null, null,
                monthlySavingsGoal, depositAmount,
                null, null, List.of()
        );
    }

    @Test
    void 요청_JSON의_category를_읽는다() {
        String json = """
                {"options": [], "detailedOptions": {"depositAmount": 1000000}, "category": "SHORT_TERM"}
                """;

        SearchRequestDto request = JsonMapper.builder().build().readValue(json, SearchRequestDto.class);

        assertThat(request.category()).isEqualTo(ProductCategoryEnum.SHORT_TERM);
    }

    @Test
    void 요청_JSON에_category가_없으면_null이다() {
        String json = """
                {"options": [], "detailedOptions": {"depositAmount": 1000000}}
                """;

        SearchRequestDto request = JsonMapper.builder().build().readValue(json, SearchRequestDto.class);

        assertThat(request.category()).isNull();
    }

    @Test
    void 요청에_대분류를_명시하면_저축기간보다_우선한다() {
        SearchRequestDto request = new SearchRequestDto(List.of(), null, ProductCategoryEnum.LONG_TERM);
        ResolvedKeywords threeMonths = new ResolvedKeywords(
                List.of(), List.of(), KeywordValueEnum.TERM_3_MONTH, List.of(), List.of());

        assertThat(request.category(threeMonths)).isEqualTo(ProductCategoryEnum.LONG_TERM);
    }

    @Test
    void 요청에_대분류가_없으면_저축기간으로_정한다() {
        SearchRequestDto request = new SearchRequestDto(List.of(), null);
        ResolvedKeywords threeMonths = new ResolvedKeywords(
                List.of(), List.of(), KeywordValueEnum.TERM_3_MONTH, List.of(), List.of());

        assertThat(request.category(threeMonths)).isEqualTo(ProductCategoryEnum.SHORT_TERM);
    }

    @Test
    void SearchRequestDto_대분류_판별은_키워드_기반이다() {
        SearchRequestDto request = new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(
                        null, null, null, null, null,
                        null, null, null,
                        null, 1000000L,
                        null, null, List.of()
                )
        );

        // 키워드로 저축기간 결정
        ResolvedKeywords shortTermKeywords = new ResolvedKeywords(
                List.of(), List.of(),
                KeywordValueEnum.TERM_1_MONTH,
                List.of(), List.of()
        );
        ResolvedKeywords longTermKeywords = new ResolvedKeywords(
                List.of(), List.of(),
                KeywordValueEnum.TERM_12_MONTH,
                List.of(), List.of()
        );

        assertThat(ProductCategoryEnum.fromSaveTrm(request.getSaveTrm(shortTermKeywords)))
                .isEqualTo(ProductCategoryEnum.SHORT_TERM);
        assertThat(ProductCategoryEnum.fromSaveTrm(request.getSaveTrm(longTermKeywords)))
                .isEqualTo(ProductCategoryEnum.LONG_TERM);

        assertThat(request.depositAmount()).isEqualTo(1000000L);
    }
}
