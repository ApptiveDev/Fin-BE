package apptive.fin.search;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.OptionRequestDto;
import apptive.fin.search.dto.ProductRateDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.dto.UnifiedSearchResultDto;
import apptive.fin.search.enums.ProductCategoryEnum;
import apptive.fin.search.service.SearchService;
import apptive.fin.support.IntegrationTestSupport;
import apptive.fin.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 결과 화면의 대분류 전환 통합 테스트.
 * 요청에 대분류를 명시하면 그 대분류로 검색하고, 대분류와 맞지 않는 저축기간은 고르지 않은 것으로 본다.
 * 픽스처: 1개월 예금 3개 + 파킹통장 1개.
 */
@Sql(
        scripts = "/sql/short-term-sorting-products.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup-product-fixtures.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CategorySwitchIntegrationTest extends IntegrationTestSupport {

    // V9 migration의 category_option 삽입 순서로 결정되는 옵션 ID
    private static final Long TERM_3_MONTH_OPTION_ID = 38L;    // TERM_3_MONTH
    private static final Long TERM_12_MONTH_OPTION_ID = 40L;   // TERM_12_MONTH
    private static final Long SALARY_TRANSFER_OPTION_ID = 32L; // BANK_SALARY_TRANSFER

    @Autowired
    private SearchService searchService;

    @Test
    void 단기예치로_전환하면_목돈_저축기간은_무시하고_단기_상품을_모두_보여준다() {
        SearchRequestDto request = new SearchRequestDto(
                List.of(
                        new OptionRequestDto(3L, TERM_12_MONTH_OPTION_ID),
                        new OptionRequestDto(6L, SALARY_TRANSFER_OPTION_ID)
                ),
                detailedOptions(null, 10_000_000L),
                ProductCategoryEnum.SHORT_TERM
        );

        UnifiedSearchResultDto result = searchService.searchUnified(request, loggedInUser());

        assertThat(result.depositSavingsProducts())
                .extracting(ProductRateDto::productName)
                .containsExactlyInAnyOrder("고금리예금", "중금리예금", "저금리예금");
    }

    @Test
    void 목돈만들기로_전환하면_단기_저축기간은_적합도_기간점수에서_빠진다() {
        // 3개월을 그대로 쓰면 1개월 상품이 인접 기간으로 기간 점수 절반을 받는다.
        SearchRequestDto request = new SearchRequestDto(
                List.of(
                        new OptionRequestDto(3L, TERM_3_MONTH_OPTION_ID),
                        new OptionRequestDto(6L, SALARY_TRANSFER_OPTION_ID)
                ),
                detailedOptions(100_000L, null),
                ProductCategoryEnum.LONG_TERM
        );

        UnifiedSearchResultDto result = searchService.searchUnified(request, loggedInUser());

        assertThat(result.productCategory()).isEqualTo(ProductCategoryEnum.LONG_TERM);
        assertThat(result.bankRanked())
                .isNotEmpty()
                .allSatisfy(dto -> assertThat(dto.periodScore()).isZero());
    }

    private DetailedOptionsDto detailedOptions(Long monthlySavingsGoal, Long depositAmount) {
        return new DetailedOptionsDto(
                LocalDate.now().minusYears(25),
                50_000_000L, 3, 100,
                null, null, null, null,
                monthlySavingsGoal,
                depositAmount,
                List.of(), List.of(), List.of()
        );
    }

    private AuthUserDetails loggedInUser() {
        AuthUserDetails userDetails = mock(AuthUserDetails.class);
        when(userDetails.getRole()).thenReturn(UserRole.RECOMMENDATION);
        return userDetails;
    }
}
