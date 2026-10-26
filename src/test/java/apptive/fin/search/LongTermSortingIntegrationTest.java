package apptive.fin.search;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.OptionRequestDto;
import apptive.fin.search.dto.ProductRateDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.dto.UnifiedSearchResultDto;
import apptive.fin.search.enums.CategoryIdEnum;
import apptive.fin.search.service.SearchService;
import apptive.fin.support.IntegrationTestSupport;
import apptive.fin.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 목돈만들기 탭B 정렬 통합 테스트.
 * 실제 SearchService.searchUnified 결과 순서를 검증한다.
 */
@Sql(
        scripts = "/sql/long-term-sorting-products.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup-product-fixtures.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class LongTermSortingIntegrationTest extends IntegrationTestSupport {

    // V9 migration의 category_option 삽입 순서로 결정되는 옵션 ID
    private static final Long TERM_12_MONTH_OPTION_ID = 40L;     // TERM_12_MONTH
    private static final Long FIRST_TRANSACTION_OPTION_ID = 31L; // BANK_FIRST_TRANSACTION

    @Autowired
    private SearchService searchService;

    @Test
    void 탭B_은행상품은_실수령액_내림차순으로_정렬된다() {
        UnifiedSearchResultDto result = searchService.searchUnified(createLongTermRequest(), authenticatedUser());

        assertThat(result.tabs().tabBEnabled()).isTrue();
        assertThat(rateNames(result.bankRateRanked()))
                .containsExactly("B고금리적금", "A저금리적금");
        assertThat(result.bankRateRanked().get(0).netReturn())
                .isGreaterThan(result.bankRateRanked().get(1).netReturn());
    }

    @Test
    void 탭B_정부상품은_환산수익률이_낮아도_기여금총액이_크면_먼저_정렬된다() {
        UnifiedSearchResultDto result = searchService.searchUnified(createLongTermRequest(), authenticatedUser());

        List<ProductRateDto> governmentRateRanked = result.governmentRateRanked();
        assertThat(rateNames(governmentRateRanked))
                .containsExactly("B장기매칭", "A단기매칭");
        assertThat(governmentRateRanked.get(0).expectedTotalContribution())
                .isGreaterThan(governmentRateRanked.get(1).expectedTotalContribution());
        assertThat(governmentRateRanked.get(0).achievableRate())
                .isLessThan(governmentRateRanked.get(1).achievableRate());
    }

    private SearchRequestDto createLongTermRequest() {
        return new SearchRequestDto(
                List.of(
                        new OptionRequestDto(CategoryIdEnum.PERIOD.getId(), TERM_12_MONTH_OPTION_ID),
                        new OptionRequestDto(CategoryIdEnum.BANK_COND.getId(), FIRST_TRANSACTION_OPTION_ID)
                ),
                new DetailedOptionsDto(
                        LocalDate.now().minusYears(27),
                        30_000_000L,   // annualIncome
                        3,             // householdSize
                        100,           // householdIncomePercent
                        12,            // tenureMonths
                        null,          // isFirstJob
                        null,          // isHomeless
                        null,          // isHouseholder
                        100_000L,      // monthlySavingsGoal
                        null,          // depositAmount
                        List.of(),     // neverUsedBanks
                        List.of(),     // maturedSavingBanks
                        List.of()      // selectedInterestRateOptions
                )
        );
    }

    private List<String> rateNames(List<ProductRateDto> products) {
        return products.stream()
                .map(ProductRateDto::productName)
                .toList();
    }

    private AuthUserDetails authenticatedUser() {
        return new AuthUserDetails(1L, UserRole.RECOMMENDATION);
    }
}
