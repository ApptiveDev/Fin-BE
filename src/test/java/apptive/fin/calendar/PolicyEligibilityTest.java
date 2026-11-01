package apptive.fin.calendar;

import apptive.fin.calendar.dto.Eligibility;
import apptive.fin.calendar.service.PolicyEligibility;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// 2계층(온통청년 정책) 자격 판정: 나이와 거주지역만 본다. 프로필 값이 비면 통과시키고 '정보 입력 필요'.
class PolicyEligibilityTest {

    private static final String BUSAN_GU = "26110,26140";
    // 온통청년이 전국 정책에 붙이는 시군구 코드 일부. 시도마다 하나씩이면 전국으로 본다.
    private static final String NATIONWIDE =
            "11110,12110,26110,27110,28110,30110,31110,36110,41111,43111,44131,47111,48121,50110,51110,52111";

    @Test
    void 나이와_지역이_맞으면_충족이다() {
        assertThat(PolicyEligibility.judge(19, 34, BUSAN_GU, 25, Set.of("REGION_BUSAN")))
                .isEqualTo(Eligibility.ELIGIBLE);
    }

    @Test
    void 나이_범위는_양_끝을_포함한다() {
        assertThat(PolicyEligibility.judge(19, 34, null, 19, Set.of())).isEqualTo(Eligibility.ELIGIBLE);
        assertThat(PolicyEligibility.judge(19, 34, null, 34, Set.of())).isEqualTo(Eligibility.ELIGIBLE);
        assertThat(PolicyEligibility.judge(19, 34, null, 18, Set.of())).isEqualTo(Eligibility.INELIGIBLE);
        assertThat(PolicyEligibility.judge(19, 34, null, 35, Set.of())).isEqualTo(Eligibility.INELIGIBLE);
    }

    @Test
    void 한쪽_나이_제한만_있어도_판정한다() {
        assertThat(PolicyEligibility.judge(null, 39, null, 40, Set.of())).isEqualTo(Eligibility.INELIGIBLE);
        assertThat(PolicyEligibility.judge(19, null, null, 60, Set.of())).isEqualTo(Eligibility.ELIGIBLE);
    }

    @Test
    void 다른_지역이면_미충족이다() {
        assertThat(PolicyEligibility.judge(null, null, BUSAN_GU, 25, Set.of("REGION_SEOUL")))
                .isEqualTo(Eligibility.INELIGIBLE);
    }

    @Test
    void 제한이_없는_정책은_프로필이_비어도_충족이다() {
        assertThat(PolicyEligibility.judge(null, null, null, null, Set.of())).isEqualTo(Eligibility.ELIGIBLE);
        assertThat(PolicyEligibility.judge(null, null, NATIONWIDE, null, Set.of())).isEqualTo(Eligibility.ELIGIBLE);
    }

    @Test
    void 제한이_있는데_프로필_값이_없으면_정보_입력_필요다() {
        assertThat(PolicyEligibility.judge(19, 34, null, null, Set.of("REGION_BUSAN")))
                .isEqualTo(Eligibility.NEEDS_INFO);
        assertThat(PolicyEligibility.judge(null, null, BUSAN_GU, 25, Set.of()))
                .isEqualTo(Eligibility.NEEDS_INFO);
    }

    // 확인된 미충족이 있으면 다른 값이 비어 있어도 미충족이다.
    @Test
    void 미충족이_정보_입력_필요보다_우선한다() {
        assertThat(PolicyEligibility.judge(19, 34, BUSAN_GU, 40, Set.of()))
                .isEqualTo(Eligibility.INELIGIBLE);
    }

    // 2026 행정구역 개편으로 광주·전남은 전남광주통합특별시(12), 강원은 51, 전북은 52 코드를 쓴다.
    @Test
    void 개편된_시도_코드도_기존_거주지역과_맞춘다() {
        assertThat(PolicyEligibility.judge(null, null, "12110", 25, Set.of("REGION_GWANGJU")))
                .isEqualTo(Eligibility.ELIGIBLE);
        assertThat(PolicyEligibility.judge(null, null, "12710", 25, Set.of("REGION_JEONNAM")))
                .isEqualTo(Eligibility.ELIGIBLE);
        assertThat(PolicyEligibility.judge(null, null, "51110", 25, Set.of("REGION_GANGWON")))
                .isEqualTo(Eligibility.ELIGIBLE);
        assertThat(PolicyEligibility.judge(null, null, "52111", 25, Set.of("REGION_JEONBUK")))
                .isEqualTo(Eligibility.ELIGIBLE);
    }

    @Test
    void 대상_지역은_거주지역_키로_돌려주고_전국이면_비운다() {
        assertThat(PolicyEligibility.regionsOf(BUSAN_GU)).containsExactly("REGION_BUSAN");
        assertThat(PolicyEligibility.regionsOf("11110,26110")).containsExactly("REGION_SEOUL", "REGION_BUSAN");
        assertThat(PolicyEligibility.regionsOf(NATIONWIDE)).isEmpty();
        assertThat(PolicyEligibility.regionsOf(null)).isEmpty();
        assertThat(PolicyEligibility.regionsOf(" ")).isEmpty();
    }
}
