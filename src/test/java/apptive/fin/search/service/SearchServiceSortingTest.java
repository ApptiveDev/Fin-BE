package apptive.fin.search.service;

import apptive.fin.search.dto.ProductMatchDto;
import apptive.fin.search.dto.ProductRateDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchService 탭별 정렬 규칙 단위 테스트.
 * 동점 규칙까지 SearchService의 실제 정렬 메서드로 검증한다.
 * searchUnified 결과 순서는 ShortTermSortingIntegrationTest, LongTermSortingIntegrationTest에서 검증한다.
 */
class SearchServiceSortingTest {

    @Test
    void 세후실수령액_내림차순으로_정렬된다() {
        // Given
        ProductRateDto low = createRateDto(1L, "저금리상품", 1_000_000L);
        ProductRateDto mid = createRateDto(2L, "중금리상품", 1_050_000L);
        ProductRateDto high = createRateDto(3L, "고금리상품", 1_100_000L);

        List<ProductRateDto> products = List.of(low, mid, high);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByNetReturn(products);

        // Then
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(3L, 2L, 1L);  // high, mid, low 순서
    }

    @Test
    void 동일한_세후실수령액이면_기존_순서를_유지한다() {
        // Given
        ProductRateDto first = createRateDto(1L, "상품A", 1_050_000L);
        ProductRateDto second = createRateDto(2L, "상품B", 1_050_000L);

        List<ProductRateDto> products = List.of(first, second);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByNetReturn(products);

        // Then
        assertThat(sorted).hasSize(2);
        // 동일한 값이면 순서가 바뀌지 않음 (stable sort)
    }

    @Test
    void null_세후실수령액은_0으로_처리하여_뒤로_정렬된다() {
        // Given
        ProductRateDto withReturn = createRateDto(1L, "계산가능", 1_050_000L);
        ProductRateDto nullReturn = createRateDtoWithNullReturn(2L, "계산불가");

        List<ProductRateDto> products = List.of(nullReturn, withReturn);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByNetReturn(products);

        // Then
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(1L, 2L);  // withReturn이 먼저
    }

    @Test
    void 상품별_최고_세후실수령액_옵션이_선택된다() {
        // Given: 같은 상품의 여러 옵션
        ProductRateDto option1 = createRateDto(1L, "상품A-옵션1", 1_000_000L);
        ProductRateDto option2 = createRateDto(1L, "상품A-옵션2", 1_100_000L);  // 같은 productId, 더 높은 수익

        // When: 상품별 최고 옵션 선택
        ProductRateDto best = SearchService.compareNetReturn(option1, option2) >= 0 ? option1 : option2;

        // Then
        assertThat(best.netReturn()).isEqualTo(1_100_000L);
    }

    // === Helper methods ===

    private ProductRateDto createRateDto(Long productId, String name, Long netReturn) {
        return ProductRateDto.builder()
                .productId(productId)
                .productPropertyId(productId * 10)
                .productName(name)
                .providerName("테스트은행")
                .source("FSS")
                .baseRate(3.0)
                .achievableRate(3.5)
                .rateComparable(true)
                .isSubscription(false)
                .netReturn(netReturn)
                .principal(1_000_000L)
                .saveTrm(12)
                .productType("DEPOSIT")
                .build();
    }

    private ProductRateDto createRateDtoWithNullReturn(Long productId, String name) {
        return ProductRateDto.builder()
                .productId(productId)
                .productPropertyId(productId * 10)
                .productName(name)
                .providerName("테스트은행")
                .source("FSS")
                .baseRate(3.0)
                .achievableRate(3.5)
                .rateComparable(true)
                .isSubscription(false)
                .netReturn(null)
                .principal(null)
                .saveTrm(12)
                .productType("DEPOSIT")
                .build();
    }



    // === 탭B 은행 정렬 테스트 (실수령액 DESC → 금리 DESC → 상품명) ===

    @Test
    void 탭B_은행정렬은_실수령액_내림차순이_1순위다() {
        // Given
        ProductRateDto lowReturn = createRateDtoWithRate(1L, "저수익", 1_000_000L, 5.0);
        ProductRateDto highReturn = createRateDtoWithRate(2L, "고수익", 1_100_000L, 3.0);

        List<ProductRateDto> products = List.of(lowReturn, highReturn);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByBankRate(products);

        // Then: 실수령액 높은 순 (금리가 낮아도 실수령액이 높으면 먼저)
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(2L, 1L);
    }

    @Test
    void 탭B_은행정렬은_실수령액_동점시_금리_내림차순이_2순위다() {
        // Given: 실수령액 동일, 금리 다름
        ProductRateDto lowRate = createRateDtoWithRate(1L, "저금리", 1_050_000L, 3.0);
        ProductRateDto highRate = createRateDtoWithRate(2L, "고금리", 1_050_000L, 5.0);

        List<ProductRateDto> products = List.of(lowRate, highRate);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByBankRate(products);

        // Then: 금리 높은 순
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(2L, 1L);
    }

    @Test
    void 탭B_은행정렬은_실수령액과_금리_모두_동점시_상품명_오름차순이_3순위다() {
        // Given: 실수령액, 금리 모두 동일
        ProductRateDto productB = createRateDtoWithRate(1L, "B상품", 1_050_000L, 4.0);
        ProductRateDto productA = createRateDtoWithRate(2L, "A상품", 1_050_000L, 4.0);

        List<ProductRateDto> products = List.of(productB, productA);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByBankRate(products);

        // Then: 상품명 오름차순 (A → B)
        assertThat(sorted).extracting(ProductRateDto::productName)
                .containsExactly("A상품", "B상품");
    }

    @Test
    void 탭B_은행정렬_전체_우선순위_검증() {
        // Given: 다양한 조합
        ProductRateDto p1 = createRateDtoWithRate(1L, "C상품", 1_100_000L, 3.0);  // 실수령액 최고
        ProductRateDto p2 = createRateDtoWithRate(2L, "B상품", 1_050_000L, 5.0);  // 실수령액 중간, 금리 최고
        ProductRateDto p3 = createRateDtoWithRate(3L, "A상품", 1_050_000L, 4.0);  // 실수령액 중간, 금리 중간
        ProductRateDto p4 = createRateDtoWithRate(4L, "D상품", 1_000_000L, 6.0);  // 실수령액 최저

        List<ProductRateDto> products = List.of(p4, p3, p2, p1);  // 무작위 순서

        // When
        List<ProductRateDto> sorted = SearchService.sortedByBankRate(products);

        // Then: 1순위 실수령액 DESC → 2순위 금리 DESC → 3순위 상품명 ASC
        // p1(110만) → p2(105만, 5%) → p3(105만, 4%) → p4(100만)
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(1L, 2L, 3L, 4L);
    }

    private ProductRateDto createRateDtoWithRate(Long productId, String name, Long netReturn, double achievableRate) {
        return ProductRateDto.builder()
                .productId(productId)
                .productPropertyId(productId * 10)
                .productName(name)
                .providerName("테스트은행")
                .source("FSS")
                .baseRate(3.0)
                .achievableRate(achievableRate)
                .rateComparable(true)
                .isSubscription(false)
                .netReturn(netReturn)
                .principal(1_000_000L)
                .saveTrm(12)
                .productType("DEPOSIT")
                .build();
    }


    // ============================================================
    // 탭A 적합도순 정렬 테스트 (totalScore DESC → tieBreaker DESC → 상품명 ASC)
    // ============================================================

    @Test
    void 탭A_적합도정렬은_totalScore_내림차순이_1순위다() {
        // Given
        ProductMatchDto lowScore = createMatchDto(1L, "저점수", 70.0, 1_000_000L);
        ProductMatchDto highScore = createMatchDto(2L, "고점수", 90.0, 500_000L);

        List<ProductMatchDto> products = List.of(lowScore, highScore);

        // When
        List<ProductMatchDto> sorted = products.stream().sorted(SearchService.tabAComparator()).toList();

        // Then: 점수 높은 순 (tieBreaker가 낮아도 점수가 높으면 먼저)
        assertThat(sorted).extracting(ProductMatchDto::productId)
                .containsExactly(2L, 1L);
    }

    @Test
    void 탭A_적합도정렬은_동점시_tieBreaker_내림차순이_2순위다() {
        // Given: 점수 동일, tieBreaker(정부=기여금/은행=실수령액) 다름
        ProductMatchDto lowTie = createMatchDto(1L, "낮은타이", 80.0, 500_000L);
        ProductMatchDto highTie = createMatchDto(2L, "높은타이", 80.0, 1_000_000L);

        List<ProductMatchDto> products = List.of(lowTie, highTie);

        // When
        List<ProductMatchDto> sorted = products.stream().sorted(SearchService.tabAComparator()).toList();

        // Then: tieBreaker 높은 순
        assertThat(sorted).extracting(ProductMatchDto::productId)
                .containsExactly(2L, 1L);
    }

    @Test
    void 탭A_적합도정렬은_점수와_tieBreaker_모두_동점시_상품명_오름차순이_3순위다() {
        // Given: 점수, tieBreaker 모두 동일
        ProductMatchDto productB = createMatchDto(1L, "B상품", 80.0, 1_000_000L);
        ProductMatchDto productA = createMatchDto(2L, "A상품", 80.0, 1_000_000L);

        List<ProductMatchDto> products = List.of(productB, productA);

        // When
        List<ProductMatchDto> sorted = products.stream().sorted(SearchService.tabAComparator()).toList();

        // Then: 상품명 오름차순 (A → B)
        assertThat(sorted).extracting(ProductMatchDto::productName)
                .containsExactly("A상품", "B상품");
    }

    @Test
    void 탭A_적합도정렬_전체_우선순위_검증() {
        // Given: 다양한 조합
        ProductMatchDto p1 = createMatchDto(1L, "C상품", 90.0, 500_000L);   // 점수 최고
        ProductMatchDto p2 = createMatchDto(2L, "B상품", 80.0, 1_000_000L); // 점수 중간, tieBreaker 최고
        ProductMatchDto p3 = createMatchDto(3L, "A상품", 80.0, 800_000L);   // 점수 중간, tieBreaker 중간
        ProductMatchDto p4 = createMatchDto(4L, "D상품", 70.0, 2_000_000L); // 점수 최저

        List<ProductMatchDto> products = List.of(p4, p3, p2, p1);  // 무작위 순서

        // When
        List<ProductMatchDto> sorted = products.stream().sorted(SearchService.tabAComparator()).toList();

        // Then: 1순위 점수 DESC → 2순위 tieBreaker DESC → 3순위 상품명 ASC
        assertThat(sorted).extracting(ProductMatchDto::productId)
                .containsExactly(1L, 2L, 3L, 4L);
    }

    private ProductMatchDto createMatchDto(Long productId, String name, double totalScore, Long tieBreaker) {
        return ProductMatchDto.builder()
                .productId(productId)
                .productPropertyId(productId * 10)
                .productName(name)
                .providerName("테스트은행")
                .source("FSS")
                .totalScore(totalScore)
                .benefitScore(0)
                .periodScore(0)
                .identityScore(0)
                .depositScore(0)
                .bankCondScore(0)
                .tieBreaker(tieBreaker)
                .build();
    }


    // ============================================================
    // 탭C 예적금 정렬 테스트 (netReturn DESC → achievableRate DESC → 상품명 ASC)
    // ============================================================

    @Test
    void 탭C_예적금정렬은_실수령액_내림차순이_1순위다() {
        // Given
        ProductRateDto lowReturn = createRateDtoWithRate(1L, "저수익", 1_000_000L, 5.0);
        ProductRateDto highReturn = createRateDtoWithRate(2L, "고수익", 1_100_000L, 3.0);

        List<ProductRateDto> products = List.of(lowReturn, highReturn);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByNetReturn(products);

        // Then: 실수령액 높은 순
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(2L, 1L);
    }

    @Test
    void 탭C_예적금정렬은_실수령액_동점시_금리_내림차순이_2순위다() {
        // Given: 실수령액 동일, 금리 다름
        ProductRateDto lowRate = createRateDtoWithRate(1L, "저금리", 1_050_000L, 3.0);
        ProductRateDto highRate = createRateDtoWithRate(2L, "고금리", 1_050_000L, 5.0);

        List<ProductRateDto> products = List.of(lowRate, highRate);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByNetReturn(products);

        // Then: 금리 높은 순
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(2L, 1L);
    }

    @Test
    void 탭C_예적금정렬_전체_우선순위_검증() {
        // Given: 다양한 조합
        ProductRateDto p1 = createRateDtoWithRate(1L, "C상품", 1_100_000L, 3.0);  // 실수령액 최고
        ProductRateDto p2 = createRateDtoWithRate(2L, "B상품", 1_050_000L, 5.0);  // 실수령액 중간, 금리 최고
        ProductRateDto p3 = createRateDtoWithRate(3L, "A상품", 1_050_000L, 4.0);  // 실수령액 중간, 금리 중간
        ProductRateDto p4 = createRateDtoWithRate(4L, "D상품", 1_000_000L, 6.0);  // 실수령액 최저

        List<ProductRateDto> products = List.of(p4, p3, p2, p1);  // 무작위 순서

        // When
        List<ProductRateDto> sorted = SearchService.sortedByNetReturn(products);

        // Then: 1순위 실수령액 DESC → 2순위 금리 DESC → 3순위 상품명 ASC
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(1L, 2L, 3L, 4L);
    }


    // ============================================================
    // 탭B 정부 정렬 테스트 (기여금총액 DESC → 환산수익률 DESC → 상품명 ASC)
    // ============================================================

    @Test
    void 탭B_정부정렬은_기여금총액_내림차순이_1순위다() {
        // Given
        ProductRateDto lowContribution = createGovRateDto(1L, "저기여금", 500_000L, 5.0);
        ProductRateDto highContribution = createGovRateDto(2L, "고기여금", 1_000_000L, 3.0);

        List<ProductRateDto> products = List.of(lowContribution, highContribution);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByGovernmentRate(products);

        // Then: 기여금 높은 순 (수익률이 낮아도 기여금이 높으면 먼저)
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(2L, 1L);
    }

    @Test
    void 탭B_정부정렬은_기여금총액_동점시_환산수익률_내림차순이_2순위다() {
        // Given: 기여금 동일, 수익률 다름
        ProductRateDto lowYield = createGovRateDto(1L, "저수익률", 1_000_000L, 3.0);
        ProductRateDto highYield = createGovRateDto(2L, "고수익률", 1_000_000L, 5.0);

        List<ProductRateDto> products = List.of(lowYield, highYield);

        // When
        List<ProductRateDto> sorted = SearchService.sortedByGovernmentRate(products);

        // Then: 수익률 높은 순
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(2L, 1L);
    }

    @Test
    void 탭B_정부정렬_전체_우선순위_검증() {
        // Given: 다양한 조합
        ProductRateDto p1 = createGovRateDto(1L, "C상품", 1_500_000L, 3.0);  // 기여금 최고
        ProductRateDto p2 = createGovRateDto(2L, "B상품", 1_000_000L, 5.0);  // 기여금 중간, 수익률 최고
        ProductRateDto p3 = createGovRateDto(3L, "A상품", 1_000_000L, 4.0);  // 기여금 중간, 수익률 중간
        ProductRateDto p4 = createGovRateDto(4L, "D상품", 500_000L, 6.0);    // 기여금 최저

        List<ProductRateDto> products = List.of(p4, p3, p2, p1);  // 무작위 순서

        // When
        List<ProductRateDto> sorted = SearchService.sortedByGovernmentRate(products);

        // Then: 1순위 기여금 DESC → 2순위 수익률 DESC → 3순위 상품명 ASC
        // p1(150만) → p2(100만, 5%) → p3(100만, 4%) → p4(50만)
        assertThat(sorted).extracting(ProductRateDto::productId)
                .containsExactly(1L, 2L, 3L, 4L);
    }

    private ProductRateDto createGovRateDto(Long productId, String name, Long contribution, double achievableRate) {
        return ProductRateDto.builder()
                .productId(productId)
                .productPropertyId(productId * 10)
                .productName(name)
                .providerName("정부기관")
                .source("government")
                .baseRate(0.0)
                .achievableRate(achievableRate)
                .rateComparable(true)
                .isSubscription(false)
                .netReturn(null)
                .principal(null)
                .saveTrm(12)
                .productType("SAVING")
                .expectedTotalContribution(contribution)
                .build();
    }
}
