package apptive.fin.calendar.service;

import apptive.fin.calendar.dto.Eligibility;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 캘린더 일정의 나이·거주지역 자격 판정. PRD상 2계층(온통청년 정책)은 나이와 거주지역만 본다.
 * 1계층(Y-Fin 정책상품)도 지금은 같은 두 조건만 본다(PRD는 Y3-2 자격 필터 전체 재사용. plan-vs-implementation 참고).
 * 프로필 값이 비어 판정할 수 없는 조건은 통과시키고 NEEDS_INFO로 알린다.
 */
public final class PolicyEligibility {

    // 사용자 거주지역(1단계 REGION_* 옵션) → 온통청년 zipCd(시군구 코드) 앞 두 자리(시도).
    // 2026 행정구역 개편으로 강원 42→51, 전북 45→52, 광주(29)·전남(46)→전남광주통합특별시(12)가 됐다.
    // 온통청년 데이터에는 새 코드만 있지만(2026-11 확인) 옛 코드도 같이 둔다.
    private static final Map<String, Set<String>> SIDO_PREFIXES_BY_REGION = new LinkedHashMap<>();

    static {
        SIDO_PREFIXES_BY_REGION.put("REGION_SEOUL", Set.of("11"));
        SIDO_PREFIXES_BY_REGION.put("REGION_BUSAN", Set.of("26"));
        SIDO_PREFIXES_BY_REGION.put("REGION_DAEGU", Set.of("27"));
        SIDO_PREFIXES_BY_REGION.put("REGION_INCHEON", Set.of("28"));
        SIDO_PREFIXES_BY_REGION.put("REGION_GWANGJU", Set.of("29", "12"));
        SIDO_PREFIXES_BY_REGION.put("REGION_DAEJEON", Set.of("30"));
        SIDO_PREFIXES_BY_REGION.put("REGION_ULSAN", Set.of("31"));
        SIDO_PREFIXES_BY_REGION.put("REGION_SEJONG", Set.of("36"));
        SIDO_PREFIXES_BY_REGION.put("REGION_GYEONGGI", Set.of("41"));
        SIDO_PREFIXES_BY_REGION.put("REGION_GANGWON", Set.of("42", "51"));
        SIDO_PREFIXES_BY_REGION.put("REGION_CHUNGBUK", Set.of("43"));
        SIDO_PREFIXES_BY_REGION.put("REGION_CHUNGNAM", Set.of("44"));
        SIDO_PREFIXES_BY_REGION.put("REGION_JEONBUK", Set.of("45", "52"));
        SIDO_PREFIXES_BY_REGION.put("REGION_JEONNAM", Set.of("46", "12"));
        SIDO_PREFIXES_BY_REGION.put("REGION_GYEONGBUK", Set.of("47"));
        SIDO_PREFIXES_BY_REGION.put("REGION_GYEONGNAM", Set.of("48"));
        SIDO_PREFIXES_BY_REGION.put("REGION_JEJU", Set.of("50"));
    }

    private PolicyEligibility() {
    }

    /**
     * 정책 대상 지역을 거주지역 키(REGION_*)로 돌려준다. 지역 제한이 없거나 모든 시도가 대상이면(전국) 빈 목록.
     */
    public static List<String> regionsOf(String regionCodes) {
        if (regionCodes == null || regionCodes.isBlank()) {
            return List.of();
        }
        Set<String> sidoPrefixes = Arrays.stream(regionCodes.split(","))
                .map(String::trim)
                .filter(code -> code.length() >= 2)
                .map(code -> code.substring(0, 2))
                .collect(Collectors.toSet());
        List<String> regions = SIDO_PREFIXES_BY_REGION.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(sidoPrefixes::contains))
                .map(Map.Entry::getKey)
                .toList();
        return regions.size() == SIDO_PREFIXES_BY_REGION.size() ? List.of() : regions;
    }

    /**
     * @param regions         대상 거주지역 키(REGION_*). 빈 목록이면 지역 제한 없음
     * @param userAge         만 나이. 프로필에 생년월일이 없으면 null
     * @param userRegionKeys  프로필의 거주지역 키(REGION_*). 없으면 빈 집합
     */
    public static Eligibility judge(
            Integer minAge,
            Integer maxAge,
            List<String> regions,
            Integer userAge,
            Set<String> userRegionKeys
    ) {
        boolean ageRestricted = minAge != null || maxAge != null;
        boolean regionRestricted = !regions.isEmpty();

        boolean ageMissing = ageRestricted && userAge == null;
        boolean regionMissing = regionRestricted && userRegionKeys.isEmpty();

        boolean ageFails = ageRestricted && !ageMissing
                && ((minAge != null && userAge < minAge) || (maxAge != null && userAge > maxAge));
        boolean regionFails = regionRestricted && !regionMissing
                && regions.stream().noneMatch(userRegionKeys::contains);

        if (ageFails || regionFails) {
            return Eligibility.INELIGIBLE;
        }
        if (ageMissing || regionMissing) {
            return Eligibility.NEEDS_INFO;
        }
        return Eligibility.ELIGIBLE;
    }
}
