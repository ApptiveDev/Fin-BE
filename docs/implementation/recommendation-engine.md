# 추천 엔진 (search 패키지)

> 기획: [Y3-1 정보 입력](../planning/y3-1-input.md), [Y3-2 추천 리스트](../planning/y3-2-recommendation-list.md) · 코드: `src/main/java/apptive/fin/search`
>
> 이 문서는 `main`(#35까지) 기준이다. draft PR #36·#37(A1~A3)이 머지되면 응답 구조(`UnifiedSearchResultDto`), 배점(V2·TOP3), 탭 구성(단기예치 탭 C·D)이 바뀐다. 바뀌는 내용 요약은 [A1~A3 문서의 구현 진행 상황](../planning/a1-a3-product-expansion.md#구현-진행-상황)에 있다.

## 한눈에 보기

| 단계 | 담당 | 핵심 |
| --- | --- | --- |
| 입력 해소 | `ResolveKeywordService` | 프론트가 보낸 `(categoryId, optionId)` 목록을 `KeywordValueEnum`으로 바꿔 `ResolvedKeywords`(지역, 신분, 저축 기간, 혜택, 은행 조건)로 묶는다 |
| 요청 검증 | `SearchRequestPolicy` | 1단계 필수값 검증, 탭 B(개인화) 사용 가능 여부 판정 |
| 자격 필터 | `EligibilityFilterService` | DB 쿼리로 1차 필터 후, 옵션(`ProductProperty`) 단위로 다시 검사 |
| 적합도 (탭 A) | `MatchScoreService` | 정부·은행 배점표로 100점 만점 점수, 빠진 항목 비례 재배분 |
| 금리 (탭 B) | `RateCalculatorService` | 정부 기여금 환산 수익률, 은행 달성 가능 금리 |
| 조립 | `SearchService`, `ProductCardSummaryService` | 상품별 최고 옵션 선택, 정렬, 카드 요약 |

데이터 단위에 주의한다. **상품(`Product`)** 하나에 **옵션(`ProductProperty`)** 이 여러 개 있다. 은행 상품은 저축 기간·금리 유형별로, 정부 상품은 기간·유형별로 옵션이 나뉜다. 자격 판정과 점수 계산은 옵션 단위로 하고, 리스트에는 상품마다 가장 좋은 옵션 하나만 남긴다.

## 1. 입력

`POST /search/products` 요청 본문은 `SearchRequestDto`다.

```json
{
  "options": [{ "categoryId": 3, "optionId": 23 }, { "categoryId": 6, "optionId": 33 }],
  "detailedOptions": {
    "monthlySavingsGoal": 300000,
    "birthdate": "2000-01-01",
    "annualIncome": 30000000,
    "householdSize": 1,
    "householdIncomePercent": 100,
    "tenureMonths": 12,
    "isFirstJob": false,
    "isHomeless": true,
    "isHouseholder": false,
    "neverUsedBanks": ["0010001"],
    "maturedSavingBanks": ["0014674"]
  }
}
```

- `options`: 1단계 키워드와 2단계 거주 지역. 카테고리 ID는 `CategoryIdEnum`(1 지역, 2 신분, 3 저축 기간, 4 혜택, 5 상품 관심사, 6 은행 거래)과 같고, 옵션은 `category_option` 테이블(`V2` 시드)에 있다. 옵션의 카테고리가 요청의 `categoryId`와 다르면 `OPTION_CATEGORY_NOT_FOUND`.
- `detailedOptions`: 월 납입 희망액과 2단계 수치 입력. 은행 목록은 금융기관 코드다.
- 로그인 사용자는 `Authorization: Bearer <JWT>`를 함께 보낸다. 비로그인도 호출할 수 있다.

### 1단계 필수값 (`validateForRecommendation`)

월 납입 희망액, 저축 기간, 은행 거래 조건 1개 이상. 하나라도 없으면 400(`MONTHLY_SAVINGS_GOAL_REQUIRED`, `SAVING_PERIOD_REQUIRED`, `BANK_CONDITION_REQUIRED`)으로 거절한다. 로그인 여부와 상관없이 적용된다.

### 탭 B 활성화 (`canUsePersonalization`)

아래를 **모두** 만족해야 탭 B와 개인화 수치가 열린다. 상품 상세와 찜 목록도 같은 함수를 쓴다.

1. 로그인 사용자이고 역할이 `RECOMMENDATION` 또는 `ADMIN` (필수 약관 동의 완료)
2. 1단계 필수값 충족
3. 생년월일, 연 소득, 가구원 수, 가구 소득(중위소득 %) 입력
4. `neverUsedBanks`, `maturedSavingBanks`가 둘 다 `null`이 아님

4번에서 빈 목록은 "해당 은행 없음"으로 답한 것으로 보고 통과시킨다. `null`만 미입력이다(#33). 빈 목록이면 첫거래·재예치 우대는 붙지 않는다.

## 2. 자격 필터 (`EligibilityFilterService`)

`ProductRepository.findEligibleProducts`가 JPQL로 1차 필터를 하고, 같은 조건을 옵션 단위(`isPropertyEligible`)로 다시 검사한다. 쿼리는 "상품 안에 조건을 만족하는 옵션이 하나라도 있는가"만 보기 때문이다.

| 조건 | 판정 (사용자 값이 `null`이면 통과) |
| --- | --- |
| 가입 가능 | `is_joinable = true` (모집 종료·단종은 수집기가 false로 둔다) |
| 나이 | `min_age ≤ 만 나이 ≤ max_age` |
| 소득 증빙 불가 | 연 소득이 0원이면 `earn_max_amt` 또는 `earn_percent`가 있는 옵션 제외 |
| 개인 소득 | `earn_max_amt ≥ 연 소득` |
| 가구 소득 | `earn_percent ≥ 사용자 중위소득 %` |
| 무주택·세대주 | `requires_homeless`, `requires_householder`가 true면 사용자도 true여야 함 |
| 근속 | `min_tenure_months ≤ 근속 개월`. 신분에 `#미취업`이 있으면 근속을 0개월로 본다 |
| 월 납입 하한 | `min_monthly_limit ≤ 월 납입 희망액` |
| 현재 신분 | 아래 참고 |

**현재 신분**은 텍스트 매칭이 아니라 수집기가 만든 `product_property_required_keyword`로 판정한다. 신뢰도 `HIGH`인 `STATUS_*` 조건만 쓴다.

- `EXCLUDE` 조건의 신분을 사용자가 골랐으면 제외
- `REQUIRE` 조건이 있으면 그중 하나 이상을 사용자가 골라야 통과
- 사용자가 신분을 고르지 않았는데 `REQUIRE`가 있으면 제외된다

**거주 지역**은 필터 후 `SearchService`에서 따로 거른다. 사용자가 지역을 골랐으면 은행 상품은 모두 남기고, 정부 상품은 옵션에 `REGION_*` 키워드가 없거나(전국) 고른 지역과 겹치는 것만 남긴다.

응답의 `eligibleProductCount`는 필터를 통과한 상품 수(중복 제거)다. "총 OO개 중 XX개 제외" 문구에 필요한 전체 상품 수는 응답에 없다.

## 3. 탭 A: 적합도 (`MatchScoreService`)

### 배점 (`ScoreWeightEnum`)

| 항목 | 정부 | 은행 |
| --- | --- | --- |
| 핵심 혜택 `benefits` | 40 | 20 |
| 저축 기간 `period` | 22 | 20 |
| 현재 신분 `identity` | 20 | 5 |
| 납입 한도 `deposit` | 18 | 15 |
| 은행 거래 `bankCond` | 0 (항상 비활성) | 40 |

각 항목은 0~1 비율을 구한 뒤 가중치를 곱한다. 총점은 다섯 항목의 합이다.

### 항목별 계산

- **핵심 혜택**: 상품 유형에 해당하는 혜택만 분모로 쓴다. 정부는 `BENEFIT_GOV_SUBSIDY`, `BENEFIT_TAX_FREE`, `BENEFIT_EASY_CONDITION`, 은행은 `BENEFIT_MAX_INTEREST`, `BENEFIT_EASY_CONDITION`. 매칭은 옵션의 키워드(`product_property_keyword`) 포함 여부로 본다.
  - 은행 `#최고이율`만 예외로, 정적 태그 대신 **이번 결과셋의 은행 옵션 최고금리 상위 30% 이상**인지로 판정한다(`BankMaxInterestPolicy`). 임계값은 최고금리 내림차순에서 `ceil(n × 0.3) − 1`번째 값이다.
- **저축 기간**: 1년 내외 = 6~12개월, 2~3년 = 24~36개월, 3년 초과 = 37개월 이상이면 1.0. 인접(1년→24, 2~3년→12, 3년 초과→36)이면 0.5, 아니면 0.
- **현재 신분**: 정부는 고른 신분이 옵션 키워드에 있고 그 신분이 특화 신분(군복무, 중소기업 재직, 미취업)이면 1.0, 그냥 포함이면 0.5. 은행은 포함이면 1.0.
- **납입 한도**: 희망액 ≤ `max_monthly_limit`(또는 한도 없음)이면 1.0, 넘으면 `한도 / 희망액`.
- **은행 거래** (은행만): 활성 조건 중 매칭된 비율.
  - 활성 조건 = 사용자가 고른 은행 조건(첫거래·재예치·기타 제외) + 옵션에 비대면 우대가 있으면 `BANK_ONLINE_JOIN` + 옵션에 청년 구간 연령 우대가 있으면 `BANK_AGE`
  - 탭 B가 활성이고 거래 이력이 있으면 `BANK_FIRST_TRANSACTION`(거래 안 한 은행 1개 이상), `BANK_REDEPOSIT`(만기 예적금 은행 1개 이상)을 추가
  - 첫거래·재예치는 옵션의 금융기관이 사용자가 고른 은행 목록에 있을 때만 매칭

### 비례 재배분 (`distributeWeights`)

아래 항목은 비활성으로 보고 가중치를 0으로 만든 뒤, 빠진 가중치 합을 남은 항목에 원래 비율대로 나눠 준다(`new = w + removed × w / activeTotal`).

- 적용 가능한 혜택을 하나도 고르지 않음
- 저축 기간 미선택
- 신분 미선택
- 월 납입 희망액 없음
- 은행 거래 활성 조건이 없음, 또는 정부 상품

### 탭 A 정렬

옵션별 점수를 계산한 뒤 `collapseToBestPerProduct`로 상품마다 총점이 가장 높은 옵션만 남기고(동점이면 먼저 계산된 것) 내림차순 정렬한다. 정부(`governmentRanked`)와 은행(`bankRanked`)은 따로 반환한다.

## 4. 탭 B: 금리 (`RateCalculatorService`)

탭 B가 비활성이면 금리 리스트는 빈 배열이다.

### 정부 상품: 기여금 환산 수익률

`exclude_from_rate_comparison = true`이거나 기여 유형이 없으면 계산하지 않는다(`rateComparable = false`, 탭 B에서 빠짐).

```text
기여 기간(월) = gov_contribution_period_months ?? save_trm
유효 월 납입  = min(월 납입 희망액, max_monthly_limit)

RATIO        : gov_matching_ratio / 기여 기간(년) × 100
FIXED_AMOUNT : (gov_monthly_fixed_contribution / 유효 월 납입) / 기여 기간(년) × 100

예상 만기 기여금 총액
RATIO        : round(gov_matching_ratio × 유효 월 납입 × 기여 기간(월))
FIXED_AMOUNT : gov_monthly_fixed_contribution × 기여 기간(월)
```

공제부금형(그룹 2) 상품은 시드 데이터에 넣지 않아 추천 대상에 없다.

### 은행 상품: 달성 가능 금리

```text
달성 가능 금리 = base_rate + Σ(적용 가능한 우대금리 rate)
```

적용 가능한 우대 조건 집합:

- 사용자가 고른 은행 조건 (첫거래·재예치·기타 제외)
- `BANK_ONLINE_JOIN`: 항상 포함
- `BANK_AGE`: 항상 포함하되, 우대금리 행의 연령 구간(`min_age`~`max_age`)이 청년 구간 19~34세와 겹칠 때만 더한다. **사용자 나이는 보지 않는다**
- 거래 이력이 있을 때: 옵션 금융기관이 `neverUsedBanks`에 있으면 `BANK_FIRST_TRANSACTION`, `maturedSavingBanks`에 있으면 `BANK_REDEPOSIT`

`BANK_ETC`(분류되지 않은 우대)는 더하지 않는다.

### 탭 B 정렬

상품마다 달성 가능 금리가 가장 높은 옵션만 남겨 내림차순 정렬한다. 정부(`governmentRateRanked`)와 은행(`bankRateRanked`)은 따로 반환하고, 청약(`ProductType.SUBSCRIPTION`)은 정렬 없이 `subscriptionProducts`로 뺀다("청약: 금리 비교 대상 아님").

## 5. 응답 (`ProductSearchResultDto`)

| 필드 | 내용 |
| --- | --- |
| `tabs` | 탭 A는 항상 활성, 탭 B 활성 여부와 비활성 사유 문구 |
| `governmentRanked`, `bankRanked` | 탭 A 정렬 결과 (항목별 점수 포함) |
| `governmentRateRanked`, `bankRateRanked` | 탭 B 정렬 결과 |
| `subscriptionProducts` | 청약 상품 |
| `productCardSummaries` | 위 리스트에 나온 옵션별 카드 요약 (뱃지 키워드, 납입·예치 한도, 적합도, 기본·최고 금리, 달성 가능 금리, 예상 만기 기여금 등). 같은 상품이라도 탭 A와 탭 B가 다른 옵션을 고르면 두 건이 된다 |
| `eligibleProductCount` | 필터 통과 상품 수 |

카드 요약은 리스트에서 계산한 값을 `productPropertyId`로 재사용하므로 리스트 수치와 카드 수치가 같다.

## 6. 동적 폼 (`POST /search/dynamic-form`)

2단계 폼 제어값을 돌려준다.

- 신분에 `#미취업`이 있으면 연 소득 기본값 0, 근속 기간 숨김
- `householdSize`가 있으면 올해(KST) 중위소득 표를 가구원 수 기준으로 반환

## 7. 상품명 검색 (`GET /search/products?searchInput=`)

가입 가능한 옵션이 있는 상품을 이름 부분 일치(대소문자 무시)로 찾고, 최고금리가 가장 높은 옵션의 기관명과 기본·최고 금리를 붙여 반환한다. 빈 입력이면 빈 배열이다.

## 일관성을 지키는 장치

같은 상품의 수치가 화면마다 달라지지 않게 하는 것이 이 패키지의 주요 제약이다.

- 리스트, 상세, 찜 목록이 모두 `SearchRequestPolicy.canUsePersonalization`으로 개인화 여부를 판정한다.
- `#최고이율` 임계값은 리스트와 상세(추천 맥락이 있을 때), 찜 목록 모두 "가입 가능한 은행 옵션 전체 결과셋"으로 계산한다. 찜한 상품만으로 계산하면 값이 달라진다(이슈 #33).
- 상세와 카드는 리스트가 고른 `productPropertyId`를 받아 같은 헬퍼로 계산한다.

관련 테스트: `MatchScoreServiceTest`, `RateCalculatorServiceTest`, `EligibilityFilterServiceIntegrationTest`, `EligibilityBoundaryIntegrationTest`, `PrdGoldenScenarioTest`(기획 예시 시나리오), `GovernmentSeedYieldIntegrationTest`(시드 정부 상품 수익률), `MyFinFitScoreParityIntegrationTest`(찜·리스트 점수 일치).
