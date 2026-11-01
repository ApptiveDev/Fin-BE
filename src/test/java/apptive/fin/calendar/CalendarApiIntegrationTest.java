package apptive.fin.calendar;

import apptive.fin.auth.security.AuthUserDetails;
import apptive.fin.category.entity.CategoryOption;
import apptive.fin.category.repository.CategoryOptionRepository;
import apptive.fin.support.IntegrationTestSupport;
import apptive.fin.user.UserRole;
import apptive.fin.user.dto.UserProfileRequestDto;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import apptive.fin.user.service.UserProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수집기가 쓰는 youth_policy, policy_product_round 테이블(V10)에 데이터를 넣고 캘린더 API 응답을 확인한다.
 * 날짜는 실행일(KST) 기준 상대값으로 넣는다.
 */
class CalendarApiIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private static final Instant SYNCED_AT = Instant.parse("2026-11-01T00:10:00Z");
    private static final String BUSAN_GU = "26110,26140";

    @Autowired private WebApplicationContext applicationContext;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileService userProfileService;
    @Autowired private CategoryOptionRepository categoryOptionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
        deletePolicies();
        jdbcTemplate.update("""
                INSERT INTO provider (source_id, code, name)
                VALUES ((SELECT id FROM product_source WHERE code = 'ONTONG'), 'CAL_PROVIDER', '캘린더 기관')
                """);
    }

    @AfterEach
    void tearDown() {
        deletePolicies();
    }

    @Test
    void 기간_안의_시작_마감_일정을_날짜순으로_돌려준다() throws Exception {
        long policy = insertPolicy("P-1", "부산 청년 자산형성", "PERIOD", true, null, null, BUSAN_GU);
        insertRound(policy, 1, TODAY.plusDays(2), TODAY.plusDays(10), null);
        // 같은 날 시작·마감이 겹치면 마감이 먼저다.
        long other = insertPolicy("P-2", "청년 신용회복", "PERIOD", true, 19, 34, null);
        insertRound(other, 1, TODAY.minusDays(20), TODAY.plusDays(2), null);

        mockMvc.perform(get("/calendar/events")
                        .queryParam("from", TODAY.toString())
                        .queryParam("to", TODAY.plusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(SYNCED_AT.toString()))
                .andExpect(jsonPath("$.events[*].type").value(contains("END", "START", "END")))
                .andExpect(jsonPath("$.events[*].round.name").value(contains("청년 신용회복", "부산 청년 자산형성", "부산 청년 자산형성")))
                .andExpect(jsonPath("$.events[1].date").value(TODAY.plusDays(2).toString()))
                .andExpect(jsonPath("$.events[1].round.scheduleKind").value("PERIOD"))
                .andExpect(jsonPath("$.events[1].round.applyStartDate").value(TODAY.plusDays(2).toString()))
                .andExpect(jsonPath("$.events[1].round.applyEndDate").value(TODAY.plusDays(10).toString()))
                .andExpect(jsonPath("$.events[1].round.regions").value(contains("REGION_BUSAN")))
                .andExpect(jsonPath("$.events[1].round.externalUrl").value(
                        "https://www.youthcenter.go.kr/youthPolicy/ythPlcyTotalSearch/ythPlcyDetail/P-1"))
                .andExpect(jsonPath("$.events[1].round.eligibility").value(nullValue()))
                .andExpect(jsonPath("$.events[0].round.minAge").value(19))
                .andExpect(jsonPath("$.events[0].round.maxAge").value(34));
    }

    @Test
    void 기간_밖의_일정과_표시하지_않는_정책은_빠진다() throws Exception {
        // 마감일만 기간 안 → 마감 일정만
        long edge = insertPolicy("P-1", "기간 걸침", "PERIOD", true, null, null, null);
        insertRound(edge, 1, TODAY.minusDays(40), TODAY.plusDays(3), null);
        // 비활성(온통청년 목록에서 사라짐), 마감 처리된 정책
        insertRound(insertPolicy("P-2", "비활성", "PERIOD", false, null, null, null), 1, TODAY, TODAY.plusDays(1), null);
        insertRound(insertPolicy("P-3", "마감", "CLOSED", true, null, null, null), 1, TODAY, TODAY.plusDays(1), null);
        // 이미 시작한 연중 접수는 그리드에 찍지 않는다
        insertRound(insertPolicy("P-4", "연중", "PERIOD", true, null, null, null), 1,
                LocalDate.of(TODAY.getYear(), 1, 1), LocalDate.of(TODAY.getYear(), 12, 31), null);

        mockMvc.perform(get("/calendar/events")
                        .queryParam("from", TODAY.toString())
                        .queryParam("to", TODAY.plusDays(10).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].type").value("END"))
                .andExpect(jsonPath("$.events[0].round.name").value("기간 걸침"));
    }

    @Test
    void 요약은_오늘_기준_신청_가능_연중_상시를_나눠_돌려준다() throws Exception {
        insertRound(insertPolicy("P-1", "늦게 마감", "PERIOD", true, null, null, null), 1,
                TODAY.minusDays(5), TODAY.plusDays(20), null);
        insertRound(insertPolicy("P-2", "곧 마감", "PERIOD", true, null, null, null), 1,
                TODAY.minusDays(5), TODAY, null);
        insertRound(insertPolicy("P-3", "아직 시작 전", "PERIOD", true, null, null, null), 1,
                TODAY.plusDays(1), TODAY.plusDays(5), null);
        insertRound(insertPolicy("P-4", "연중 접수", "PERIOD", true, null, null, null), 1,
                LocalDate.of(TODAY.getYear(), 1, 1), LocalDate.of(TODAY.getYear(), 12, 31), null);
        insertPolicy("P-5", "상시 신청", "ALWAYS", true, null, null, null);
        insertPolicy("P-6", "비활성 상시", "ALWAYS", false, null, null, null);

        mockMvc.perform(get("/calendar/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(SYNCED_AT.toString()))
                .andExpect(jsonPath("$.openNow[*].name").value(contains("곧 마감", "늦게 마감")))
                .andExpect(jsonPath("$.yearRound[*].name").value(contains("연중 접수")))
                .andExpect(jsonPath("$.yearRound[0].scheduleKind").value("YEAR_ROUND"))
                .andExpect(jsonPath("$.always[*].name").value(contains("상시 신청")))
                .andExpect(jsonPath("$.always[0].scheduleKind").value("ALWAYS"))
                .andExpect(jsonPath("$.always[0].roundId").value(nullValue()))
                .andExpect(jsonPath("$.always[0].applyStartDate").value(nullValue()));
    }

    @Test
    void 로그인_사용자는_프로필로_자격을_판정받는다() throws Exception {
        insertRound(insertPolicy("P-1", "부산 25세 대상", "PERIOD", true, 19, 34, BUSAN_GU), 1,
                TODAY.minusDays(1), TODAY.plusDays(1), null);
        insertRound(insertPolicy("P-2", "서울 대상", "PERIOD", true, null, null, "11110"), 1,
                TODAY.minusDays(1), TODAY.plusDays(2), null);
        insertRound(insertPolicy("P-3", "30세 이상", "PERIOD", true, 30, null, null), 1,
                TODAY.minusDays(1), TODAY.plusDays(3), null);
        Long userId = createUserWithProfile(TODAY.minusYears(25), "REGION_BUSAN");

        mockMvc.perform(get("/calendar/summary").with(user(new AuthUserDetails(userId, UserRole.RECOMMENDATION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openNow[*].eligibility").value(contains("ELIGIBLE", "INELIGIBLE", "INELIGIBLE")));

        // 프로필이 없으면 제한 있는 정책은 모두 정보 입력 필요
        Long noProfileUser = createUser();
        mockMvc.perform(get("/calendar/summary").with(user(new AuthUserDetails(noProfileUser, UserRole.RECOMMENDATION))))
                .andExpect(jsonPath("$.openNow[*].eligibility").value(contains("NEEDS_INFO", "NEEDS_INFO", "NEEDS_INFO")));

        // 약관 동의 전 사용자는 판정하지 않는다
        mockMvc.perform(get("/calendar/summary").with(user(new AuthUserDetails(userId, UserRole.BEFORE_AGREED))))
                .andExpect(jsonPath("$.openNow[0].eligibility").value(nullValue()));
    }

    @Test
    void 일정_변경_표시는_7일간만_켜진다() throws Exception {
        insertRound(insertPolicy("P-1", "최근 변경", "PERIOD", true, null, null, null), 1,
                TODAY.minusDays(1), TODAY.plusDays(1), Instant.now().minus(6, ChronoUnit.DAYS));
        insertRound(insertPolicy("P-2", "오래전 변경", "PERIOD", true, null, null, null), 1,
                TODAY.minusDays(1), TODAY.plusDays(2), Instant.now().minus(8, ChronoUnit.DAYS));

        mockMvc.perform(get("/calendar/summary"))
                .andExpect(jsonPath("$.openNow[*].scheduleChanged").value(contains(true, false)));
    }

    @Test
    void 정책상품_회차는_1계층으로_나오고_같은_날_같은_유형이면_먼저다() throws Exception {
        // 옵션마다 나이가 다르면 가장 넓은 범위(19~39)를 쓴다.
        long product = insertProduct("CAL_POLICY_1", "하나 청년통장", true, "REGION_BUSAN",
                new Integer[]{19, 34}, new Integer[]{19, 39});
        long round = insertProductRound(product, 1, TODAY.plusDays(2), TODAY.plusDays(10));
        // 이름은 앞서지만 2계층이라 같은 날 마감 중 뒤에 온다.
        insertRound(insertPolicy("P-1", "가나 청년정책", "PERIOD", true, null, null, null), 1,
                TODAY.minusDays(3), TODAY.plusDays(10), null);

        mockMvc.perform(get("/calendar/events")
                        .queryParam("from", TODAY.toString())
                        .queryParam("to", TODAY.plusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[*].round.name").value(contains("하나 청년통장", "하나 청년통장", "가나 청년정책")))
                .andExpect(jsonPath("$.events[*].type").value(contains("START", "END", "END")))
                .andExpect(jsonPath("$.events[1].round.tier").value("POLICY_PRODUCT"))
                .andExpect(jsonPath("$.events[1].round.productId").value(product))
                .andExpect(jsonPath("$.events[1].round.roundId").value(round))
                .andExpect(jsonPath("$.events[1].round.policyId").value(nullValue()))
                .andExpect(jsonPath("$.events[1].round.externalUrl").value(nullValue()))
                .andExpect(jsonPath("$.events[1].round.institution").value("캘린더 기관"))
                .andExpect(jsonPath("$.events[1].round.minAge").value(19))
                .andExpect(jsonPath("$.events[1].round.maxAge").value(39))
                .andExpect(jsonPath("$.events[1].round.regions").value(contains("REGION_BUSAN")))
                .andExpect(jsonPath("$.events[2].round.tier").value("YOUTH_POLICY"));
    }

    @Test
    void 가입_가능한_옵션이_없는_정책상품은_빠지고_로그인하면_자격을_판정한다() throws Exception {
        insertProductRound(insertProduct("CAL_POLICY_1", "부산 통장", true, "REGION_BUSAN", new Integer[]{19, 34}),
                1, TODAY.minusDays(1), TODAY.plusDays(1));
        insertProductRound(insertProduct("CAL_POLICY_2", "서울 통장", true, "REGION_SEOUL", new Integer[]{19, 34}),
                1, TODAY.minusDays(1), TODAY.plusDays(2));
        insertProductRound(insertProduct("CAL_POLICY_3", "판매 종료 통장", false, null, new Integer[]{19, 34}),
                1, TODAY.minusDays(1), TODAY.plusDays(3));
        Long userId = createUserWithProfile(TODAY.minusYears(25), "REGION_BUSAN");

        mockMvc.perform(get("/calendar/summary").with(user(new AuthUserDetails(userId, UserRole.RECOMMENDATION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openNow[*].name").value(contains("부산 통장", "서울 통장")))
                .andExpect(jsonPath("$.openNow[*].tier").value(contains("POLICY_PRODUCT", "POLICY_PRODUCT")))
                .andExpect(jsonPath("$.openNow[*].eligibility").value(contains("ELIGIBLE", "INELIGIBLE")));
    }

    @Test
    void 잘못된_기간은_400이다() throws Exception {
        mockMvc.perform(get("/calendar/events")
                        .queryParam("from", TODAY.toString())
                        .queryParam("to", TODAY.minusDays(1).toString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/calendar/events")
                        .queryParam("from", "2026-13-01")
                        .queryParam("to", TODAY.toString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/calendar/events").queryParam("from", TODAY.toString()))
                .andExpect(status().isBadRequest());
    }

    private long insertPolicy(String policyNo, String name, String applyPeriodType, boolean active,
                              Integer minAge, Integer maxAge, String regionCodes) {
        return jdbcTemplate.queryForObject("""
                        INSERT INTO youth_policy (policy_no, name, institution, summary, min_age, max_age, region_codes,
                                                  apply_period_type, is_active, synced_at, created_at)
                        VALUES (?, ?, '기관', '설명', ?, ?, ?, ?, ?, ?, now())
                        RETURNING id
                        """, Long.class,
                policyNo, name, minAge, maxAge, regionCodes, applyPeriodType, active, Timestamp.from(SYNCED_AT));
    }

    private void insertRound(long policyId, int roundNo, LocalDate start, LocalDate end, Instant scheduleChangedAt) {
        jdbcTemplate.update("""
                        INSERT INTO youth_policy_round (policy_id, round_no, apply_start_date, apply_end_date,
                                                        schedule_changed_at, created_at)
                        VALUES (?, ?, ?, ?, ?, now())
                        """,
                policyId, roundNo, Date.valueOf(start), Date.valueOf(end),
                scheduleChangedAt == null ? null : Timestamp.from(scheduleChangedAt));
    }

    // 정부상품(source ONTONG)과 옵션 하나씩. ageRanges마다 옵션을 하나 만든다.
    private long insertProduct(String code, String name, boolean joinable, String regionKeyword, Integer[]... ageRanges) {
        long productId = jdbcTemplate.queryForObject("""
                        INSERT INTO product (source_id, type, product_code, product_name, content)
                        VALUES ((SELECT id FROM product_source WHERE code = 'ONTONG'), 'SAVING', ?, ?, '')
                        RETURNING id
                        """, Long.class, code, name);
        for (Integer[] ages : ageRanges) {
            long propertyId = jdbcTemplate.queryForObject("""
                            INSERT INTO product_properties
                                (product_id, provider_id, min_age, max_age, is_joinable, requires_homeless,
                                 requires_householder, allows_military_age_extension, exclude_from_rate_comparison)
                            VALUES (?, (SELECT id FROM provider WHERE code = 'CAL_PROVIDER'), ?, ?, ?, false, false, false, false)
                            RETURNING id
                            """, Long.class, productId, ages[0], ages[1], joinable);
            if (regionKeyword != null) {
                jdbcTemplate.update(
                        "INSERT INTO product_property_keyword (product_property_id, keyword_code) VALUES (?, ?)",
                        propertyId, regionKeyword);
            }
        }
        return productId;
    }

    private long insertProductRound(long productId, int roundNo, LocalDate start, LocalDate end) {
        return jdbcTemplate.queryForObject("""
                        INSERT INTO policy_product_round (product_id, round_no, apply_start_date, apply_end_date, created_at)
                        VALUES (?, ?, ?, ?, now())
                        RETURNING id
                        """, Long.class, productId, roundNo, Date.valueOf(start), Date.valueOf(end));
    }

    private void deletePolicies() {
        jdbcTemplate.update("DELETE FROM youth_policy");
        // 상품을 지우면 옵션, 키워드, 회차가 함께 지워진다(ON DELETE CASCADE).
        jdbcTemplate.update("DELETE FROM product WHERE product_code LIKE 'CAL_%'");
        jdbcTemplate.update("DELETE FROM provider WHERE code = 'CAL_PROVIDER'");
    }

    private Long createUserWithProfile(LocalDate birthdate, String regionCode) {
        Long userId = createUser();
        Long regionOptionId = categoryOptionRepository.findAll().stream()
                .filter(option -> regionCode.equals(option.getCode()))
                .map(CategoryOption::getId)
                .findFirst()
                .orElseThrow();
        userProfileService.upsert(userId, new UserProfileRequestDto(
                birthdate, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(regionOptionId)));
        return userId;
    }

    private Long createUser() {
        return userRepository.save(User.builder()
                .name("calendar-tester")
                .email("calendar-tester-" + System.nanoTime() + "@example.com")
                .provider("google")
                .providerId("pid-" + System.nanoTime())
                .userRole(UserRole.RECOMMENDATION)
                .build()).getId();
    }
}
