package apptive.fin.search;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.myfin.dto.MyfinResponseDto;
import apptive.fin.myfin.entity.MyFin;
import apptive.fin.myfin.repository.MyFinRepository;
import apptive.fin.myfin.service.MyFinService;
import apptive.fin.search.dto.DetailedOptionsDto;
import apptive.fin.search.dto.OptionRequestDto;
import apptive.fin.search.dto.ProductDetailRequestDto;
import apptive.fin.search.dto.ProductMatchDto;
import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.enums.CategoryIdEnum;
import apptive.fin.search.enums.ProductCategoryEnum;
import apptive.fin.search.repository.ProductPropertyRepository;
import apptive.fin.search.service.ProductDetailService;
import apptive.fin.search.service.SearchService;
import apptive.fin.support.IntegrationTestSupport;
import apptive.fin.user.UserRole;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * #58 회귀 테스트: 대분류를 명시한 목돈 검색에서 상세·찜이 추천 목록과 같은 적합도를 내는지 확인한다.
 * 목록은 대분류와 맞지 않는 저축기간을 지우고, 기간이 없으면 목돈 기간 옵션만 후보로 둔다.
 * 상세·찜도 같은 후보로 #최고이율 컷을 계산해야 한다.
 *
 * 픽스처: 1개월 예금 3개 + 파킹통장 + 12개월 옵션(고금리예금 4.5, 1년예금 5.5) + 고금리 1개월 예금 2개(6.0, 5.8).
 * - 목돈 후보만: {5.5, 4.5, 파킹 3.0, 2.5} → 컷 4.5 → 고금리예금(12개월, 4.5)이 #최고이율 충족
 * - 1개월 옵션까지 넣으면: 컷 5.5 → 고금리예금 미충족 (고치기 전 상세·찜이 이렇게 계산했다)
 */
@Sql(
        scripts = {"/sql/short-term-sorting-products.sql", "/sql/category-switch-long-term-options.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(scripts = "/sql/cleanup-product-fixtures.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CategoryParityIntegrationTest extends IntegrationTestSupport {

    // migration의 category_option 삽입 순서로 결정되는 옵션 ID
    private static final Long MAX_INTEREST_BENEFIT_OPTION_ID = 25L; // BENEFIT_MAX_INTEREST
    private static final Long SALARY_TRANSFER_OPTION_ID = 32L;      // BANK_SALARY_TRANSFER
    private static final Long TERM_3_MONTH_OPTION_ID = 38L;         // TERM_3_MONTH

    private static final List<OptionRequestDto> CATEGORY_ONLY = List.of(
            new OptionRequestDto(CategoryIdEnum.BENEFIT.getId(), MAX_INTEREST_BENEFIT_OPTION_ID),
            new OptionRequestDto(CategoryIdEnum.BANK_COND.getId(), SALARY_TRANSFER_OPTION_ID)
    );
    private static final List<OptionRequestDto> SHORT_PERIOD_SWITCHED_TO_LONG = List.of(
            new OptionRequestDto(CategoryIdEnum.PERIOD.getId(), TERM_3_MONTH_OPTION_ID),
            new OptionRequestDto(CategoryIdEnum.BENEFIT.getId(), MAX_INTEREST_BENEFIT_OPTION_ID),
            new OptionRequestDto(CategoryIdEnum.BANK_COND.getId(), SALARY_TRANSFER_OPTION_ID)
    );

    @Autowired
    private SearchService searchService;
    @Autowired
    private ProductDetailService productDetailService;
    @Autowired
    private MyFinService myFinService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MyFinRepository myFinRepository;
    @Autowired
    private ProductPropertyRepository productPropertyRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AuthUserDetails user;

    @BeforeEach
    void setUp() {
        insertHighRateOneMonthDeposit("SHORT_TOP_1", 6.0);
        insertHighRateOneMonthDeposit("SHORT_TOP_2", 5.8);
        user = new AuthUserDetails(createUser(), UserRole.RECOMMENDATION);
    }

    @Test
    void 대분류만_명시한_목돈_검색은_상세가_대분류를_안_받아도_목록과_적합도가_같다() {
        SearchRequestDto request = longTermRequest(CATEGORY_ONLY);
        ProductMatchDto card = card(request, "고금리예금");

        // 기간이 없으면 상세도 목돈으로 정해지므로, FE가 대분류를 보내기 전에도 맞아야 한다.
        double detailScore = productDetailService.getProductDetail(
                card.productId(),
                new ProductDetailRequestDto(card.productPropertyId(), request.options(), request.detailedOptions()),
                user
        ).matchScore();

        assertThat(detailScore).isCloseTo(card.totalScore(), offset(0.0001));
    }

    @Test
    void 단기_기간을_고르고_목돈으로_전환하면_상세는_대분류를_받아_목록과_적합도가_같다() {
        SearchRequestDto request = longTermRequest(SHORT_PERIOD_SWITCHED_TO_LONG);
        ProductMatchDto card = card(request, "고금리예금");

        double detailScore = productDetailService.getProductDetail(
                card.productId(),
                new ProductDetailRequestDto(
                        card.productPropertyId(), request.options(), request.detailedOptions(), request.category()),
                user
        ).matchScore();

        assertThat(detailScore).isCloseTo(card.totalScore(), offset(0.0001));
    }

    @Test
    void 대분류만_명시한_목돈_검색은_찜_적합도도_목록과_같다() {
        SearchRequestDto request = longTermRequest(CATEGORY_ONLY);
        ProductMatchDto card = card(request, "고금리예금");

        assertThat(favoriteFitScore(request, card)).isEqualTo((int) card.totalScore());
    }

    @Test
    void 단기_기간을_고르고_목돈으로_전환해도_찜_적합도는_목록과_같다() {
        SearchRequestDto request = longTermRequest(SHORT_PERIOD_SWITCHED_TO_LONG);
        ProductMatchDto card = card(request, "고금리예금");

        assertThat(favoriteFitScore(request, card)).isEqualTo((int) card.totalScore());
    }

    private ProductMatchDto card(SearchRequestDto request, String productName) {
        ProductMatchDto card = searchService.searchUnified(request, user).bankRanked().stream()
                .filter(match -> match.productName().equals(productName))
                .findFirst()
                .orElseThrow();
        assertThat(card.benefitScore()).as("전제: 목돈 후보 기준 컷 4.5를 넘는다").isPositive();
        return card;
    }

    private int favoriteFitScore(SearchRequestDto request, ProductMatchDto card) {
        myFinRepository.save(MyFin.builder()
                .user(userRepository.getReferenceById(user.getId()))
                .productProperty(productPropertyRepository.findByIdAndIsJoinableTrue(card.productPropertyId()).orElseThrow())
                .build());
        MyfinResponseDto.List_ favorites = myFinService.getFavorites(user.getId(), request, user);
        return favorites.items().stream()
                .filter(item -> item.productPropertyId().equals(card.productPropertyId()))
                .findFirst()
                .orElseThrow()
                .fitScore();
    }

    private SearchRequestDto longTermRequest(List<OptionRequestDto> options) {
        return new SearchRequestDto(
                options,
                new DetailedOptionsDto(
                        LocalDate.now().minusYears(25),
                        50_000_000L, 3, 100,
                        null, null, null, null,
                        100_000L,
                        null,
                        List.of(), List.of(), List.of()
                ),
                ProductCategoryEnum.LONG_TERM
        );
    }

    private void insertHighRateOneMonthDeposit(String productCode, double maxRate) {
        jdbcTemplate.update("""
                INSERT INTO product (source_id, type, product_code, product_name, content)
                VALUES ((SELECT id FROM product_source WHERE code = 'FSS'), 'DEPOSIT', ?, ?, '')
                """, productCode, productCode);
        jdbcTemplate.update("""
                INSERT INTO product_properties
                    (product_id, provider_id, base_rate, max_rate, min_age, max_age, is_joinable, intr_rate_type, save_trm)
                VALUES ((SELECT id FROM product WHERE product_code = ?),
                        (SELECT id FROM provider WHERE code = 'SHORT_BANK_C'), ?, ?, 17, 100, true, 'SINGLE_INTEREST', 1)
                """, productCode, maxRate, maxRate);
    }

    private Long createUser() {
        return userRepository.save(User.builder()
                .name("parity-tester")
                .email("parity-tester@example.com")
                .provider("google")
                .providerId("pid-" + System.nanoTime())
                .userRole(UserRole.RECOMMENDATION)
                .build()).getId();
    }
}
