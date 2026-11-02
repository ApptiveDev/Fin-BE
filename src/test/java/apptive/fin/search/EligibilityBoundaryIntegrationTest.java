package apptive.fin.search;

import apptive.fin.search.enums.CategoryIdEnum;
import apptive.fin.search.enums.KeywordValueEnum;
import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.OptionRequestDto;
import apptive.fin.search.dto.ResolvedKeywords;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.service.EligibilityFilterService;
import apptive.fin.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 자격요건 경계값 테스트.
 * age/annualIncome/householdIncomePercent/monthlyDeposit 각각의 경계값(=, =+1, =-1)에서
 * EligibilityFilterService(및 ProductRepository.findEligibleProducts JPQL)의 필터링이
 * 정확히 동작하는지 검증한다.
 */
@Sql(
        scripts = "/sql/eligibility-boundary-products.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup-product-fixtures.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class EligibilityBoundaryIntegrationTest extends IntegrationTestSupport {

    private static final Long MILITARY_IDENTITY_OPTION_ID = 21L; // STATUS_MILITARY (군복무)

    @Autowired
    private EligibilityFilterService eligibilityFilterService;

    // ===== 1. 나이 경계 (병역 연령 확장) =====

    @Test
    void 병역연령확장_인정상품_만34세_비군필은_포함된다() {
        List<Product> result = filter(ageRequest(34, false));

        assertThat(productCodes(result)).contains("TEST_AGE_MIL_EXT");
    }

    @Test
    void 병역연령확장_인정상품_만35세_비군필은_제외된다() {
        List<Product> result = filter(ageRequest(35, false));

        assertThat(productCodes(result)).doesNotContain("TEST_AGE_MIL_EXT");
    }

    @Test
    void 병역연령확장_표시상품도_만35세_군필은_제외된다() {
        List<Product> result = filter(ageRequest(35, true));

        assertThat(productCodes(result)).doesNotContain("TEST_AGE_MIL_EXT");
    }

    @Test
    void 병역연령확장_표시상품도_만39세_군필은_제외된다() {
        List<Product> result = filter(ageRequest(39, true));

        assertThat(productCodes(result)).doesNotContain("TEST_AGE_MIL_EXT");
    }

    @Test
    void 병역연령확장_인정상품_만40세_군필은_제외된다() {
        List<Product> result = filter(ageRequest(40, true));

        assertThat(productCodes(result)).doesNotContain("TEST_AGE_MIL_EXT");
    }

    @Test
    void 병역연령확장_미인정상품은_군필이어도_만35세면_제외된다() {
        List<Product> result = filter(ageRequest(35, true));

        assertThat(productCodes(result)).doesNotContain("TEST_AGE_NO_MIL_EXT");
    }

    // ===== 2. 소득 경계 =====

    @Test
    void 소득상한과_정확히_같으면_포함된다() {
        List<Product> result = filter(incomeRequest(24_000_000L));

        assertThat(productCodes(result)).contains("TEST_INCOME_MAX");
    }

    @Test
    void 소득상한을_1원_초과하면_제외된다() {
        List<Product> result = filter(incomeRequest(24_000_001L));

        assertThat(productCodes(result)).doesNotContain("TEST_INCOME_MAX");
    }

    @Test
    void 소득증빙불가_0원이면_소득요건_있는_상품은_제외된다() {
        List<Product> result = filter(incomeRequest(0L));

        assertThat(productCodes(result)).doesNotContain("TEST_INCOME_MAX");
    }

    @Test
    void 소득요건_없는_상품은_소득증빙불가_0원이어도_포함된다() {
        List<Product> result = filter(incomeRequest(0L));

        assertThat(productCodes(result)).contains("TEST_INCOME_NONE");
    }

    // ===== 3. 가구 중위소득 경계 =====

    @Test
    void 중위소득_비율상한과_정확히_같으면_포함된다() {
        List<Product> result = filter(percentRequest(120));

        assertThat(productCodes(result)).contains("TEST_INCOME_PERCENT");
    }

    @Test
    void 중위소득_비율상한을_초과하면_제외된다() {
        List<Product> result = filter(percentRequest(121));

        assertThat(productCodes(result)).doesNotContain("TEST_INCOME_PERCENT");
    }

    // ===== 4. 최소납입 경계 =====

    @Test
    void 최소납입액과_정확히_같으면_포함된다() {
        List<Product> result = filter(monthlyDepositRequest(100_000L));

        assertThat(productCodes(result)).contains("TEST_MONTHLY_MIN");
    }

    @Test
    void 최소납입액보다_적으면_제외된다() {
        List<Product> result = filter(monthlyDepositRequest(99_999L));

        assertThat(productCodes(result)).doesNotContain("TEST_MONTHLY_MIN");
    }

    // ===== 5. 예치금 하한 (예금=예치 총액, 적금=월 납입액) =====

    @Test
    void 단기_예금은_예치액이_최소예치금과_같으면_포함된다() {
        assertThat(optionCodes(depositAmountRequest(1_000_000L), KeywordValueEnum.TERM_3_MONTH))
                .contains("TEST_DEPOSIT_MIN_3M");
    }

    @Test
    void 단기_예금은_예치액이_최소예치금보다_적으면_제외된다() {
        assertThat(optionCodes(depositAmountRequest(999_999L), KeywordValueEnum.TERM_3_MONTH))
                .doesNotContain("TEST_DEPOSIT_MIN_3M");
    }

    @Test
    void 단기_적금은_예치액을_기간으로_나눈_월납입액이_최소납입액과_같으면_포함된다() {
        // 30만원 ÷ 3개월 = 월 10만원
        assertThat(optionCodes(depositAmountRequest(300_000L), KeywordValueEnum.TERM_3_MONTH))
                .contains("TEST_SAVING_MIN_3M");
    }

    @Test
    void 단기_적금은_예치액을_기간으로_나눈_월납입액이_최소납입액보다_적으면_제외된다() {
        // 29만원 ÷ 3개월 ≈ 월 9.6만원 (예치 총액 29만원은 월 최소 10만원보다 크지만 월 기준으로는 미달)
        assertThat(optionCodes(depositAmountRequest(290_000L), KeywordValueEnum.TERM_3_MONTH))
                .doesNotContain("TEST_SAVING_MIN_3M");
    }

    @Test
    void 목돈_예금은_월저축액에_기간을_곱한_원금이_최소예치금보다_적으면_제외된다() {
        // 월 9만원 × 12개월 = 108만원 < 최소 120만원
        assertThat(optionCodes(monthlyDepositRequest(90_000L), KeywordValueEnum.TERM_12_MONTH))
                .doesNotContain("TEST_DEPOSIT_MIN_12M");
        // 월 10만원 × 12개월 = 120만원
        assertThat(optionCodes(monthlyDepositRequest(100_000L), KeywordValueEnum.TERM_12_MONTH))
                .contains("TEST_DEPOSIT_MIN_12M");
    }

    private List<String> optionCodes(SearchRequestDto request, KeywordValueEnum savingPeriod) {
        ResolvedKeywords keywords = new ResolvedKeywords(List.of(), List.of(), savingPeriod, List.of(), List.of());
        return eligibilityFilterService.filterEligibleOptions(request, keywords).stream()
                .map(option -> option.product().getProductCode())
                .toList();
    }

    private SearchRequestDto depositAmountRequest(long depositAmount) {
        return new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(
                        birthdateForAge(30),
                        null, null, null, null, null, null, null,
                        null,
                        depositAmount,
                        null, null, List.of()
                )
        );
    }

    private List<Product> filter(SearchRequestDto request) {
        return eligibilityFilterService.filterEligible(request);
    }

    /**
     * 정확한 나이를 만들기 위해 생일을 하루 더 과거로 이동시킨다.
     * (오늘이 생일인 경우 minusYears(age)만으로는 경계가 흔들릴 수 있음)
     */
    private LocalDate birthdateForAge(int age) {
        return LocalDate.now().minusYears(age).minusDays(1);
    }

    private SearchRequestDto ageRequest(int age, boolean isMilitary) {
        List<OptionRequestDto> options = isMilitary
                ? List.of(new OptionRequestDto(CategoryIdEnum.IDENTITY.getId(), MILITARY_IDENTITY_OPTION_ID))
                : List.of();

        return new SearchRequestDto(
                options,
                new DetailedOptionsDto(
                        birthdateForAge(age),
                        null, null, null, null, null, null, null, null, List.of()
                )
        );
    }

    private SearchRequestDto incomeRequest(long annualIncome) {
        return new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(
                        birthdateForAge(30),
                        annualIncome,
                        null, null, null, null, null, null, null, List.of()
                )
        );
    }

    private SearchRequestDto percentRequest(int householdIncomePercent) {
        return new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(
                        birthdateForAge(30),
                        null,
                        null,
                        householdIncomePercent,
                        null, null, null, null, null, List.of()
                )
        );
    }

    private SearchRequestDto monthlyDepositRequest(long monthlyDeposit) {
        return new SearchRequestDto(
                List.of(),
                new DetailedOptionsDto(
                        birthdateForAge(30),
                        null, null, null, null, null, null, null,
                        monthlyDeposit,
                        List.of()
                )
        );
    }

    private List<String> productCodes(List<Product> products) {
        return products.stream()
                .map(Product::getProductCode)
                .toList();
    }
}
