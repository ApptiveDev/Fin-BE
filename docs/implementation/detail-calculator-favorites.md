# 상품 상세, 계산기, 찜, 프로필

> 기획: [Y4 상세·계산기·아웃링크](../planning/y4-product-detail.md), [Y5 마이페이지](../planning/y5-mypage.md) · 코드: `search/service/ProductDetailService.java`, `search/util`, `calculator`, `myfin`, `user/service/UserProfileService.java`

## 상품 상세 (`POST /search/products/{productId}/detail`)

공개 API다. 본문은 생략할 수 있고, 넣을 때는 `{ productPropertyId, options, detailedOptions }` 형태다(추천 요청과 같은 입력 + 리스트가 고른 옵션 ID).

### 옵션 선택

- 가입 가능한 옵션이 하나라도 있으면 그 옵션들만 후보로 쓴다. 모두 마감이면(보관 상품) 전체 옵션을 후보로 쓴다.
- `productPropertyId`가 있으면 그 옵션을 고른다. 없는 ID면 404(`PRODUCT_NOT_FOUND`).
- `productPropertyId`가 없으면 공시 최고금리 → 기본금리 → ID 순으로 대표 옵션을 고른다.

### 수치 잠금

`metricsLocked`는 다음 중 하나면 true다.

- `productPropertyId`를 보내지 않음
- `SearchRequestPolicy.canUsePersonalization`이 false (비로그인, 약관 미동의, 1·2단계 필수값 미입력)

잠기면 `lockMessage`를 주고, 정부 상품의 `government`(환산 수익률, 예상 만기 기여금, 매칭 유형, 유효 월 납입, 기여 기간)는 null, 은행 상품의 `bank`는 기본·최고금리만 채우고 달성 가능 금리와 충족·미충족 우대조건은 비운다. 청약 상품은 수치를 보여주지 않는다.

`matchScore`(적합도)는 잠금과 별개로 `productPropertyId`와 `options`가 있으면 계산한다. 리스트와 같은 값이 나오도록 거래 이력 반영 여부와 `#최고이율` 임계값을 리스트와 같은 규칙으로 맞춘다.

### 금리표 (`rateTable`)

옵션별 저축 기간, 기본·최고 금리, 이자 방식, 우대조건 목록. 정부 상품은 기본 또는 최고금리가 공시된 옵션만 넣는다(기획의 "공시 상품 한정 금리 안내").

### 신청 링크 (`ApplyLinkResolver`)

| 순서 | 조건 | 결과 |
| --- | --- | --- |
| 1 | 옵션이 마감 (`is_joinable = false`) | 모두 null, `applyStatus = RECRUIT_CLOSED` |
| 2 | 옵션에 `apply_url` 있음 | `applyUrl` |
| 3 | 금융기관에 `apply_url` 있음 | `officialChannelUrl` (기관 공식 채널) |
| 4 | 그 외 | 모두 null |

찜 등 CTA가 있는 다른 응답도 이 함수를 같이 쓴다.

### 키워드 칩

`ProductDisplayKeywordService`가 옵션 키워드를 모으고, `#최고이율`은 정적 태그 대신 상위 30% 임계값으로 다시 판정한다. 임계값 모집단은 추천 맥락(옵션 ID와 입력이 있음)이면 그 사용자의 가입 가능 은행 옵션, 아니면 가입 가능한 전체 은행 옵션이다.

## 수익률 계산기 (`POST /calculator`)

`RECOMMENDATION`, `ADMIN` 역할만 쓸 수 있다.

### 요청

| 필드 | 설명 |
| --- | --- |
| `productPropertyId` | 검증 기준 옵션 |
| `productType` | `DEPOSIT` 또는 `SAVING`. `RateCalculatorFactory`가 계산기를 고른다 |
| `interestRateType` | `SINGLE_INTEREST`(단리) / `COMPOUND_INTEREST`(월복리) |
| `reserveType` | 적금이면 필수 |
| `appliedRate` | 적용 연이율, **소수** (4% → 0.04). 옵션 최고금리 이하 |
| `amount` | 예금은 예치금, 적금은 월 납입액. 1만 원 이상, 옵션 한도(예금 `max_deposit_amount`, 적금 `max_monthly_limit`) 이하 |
| `saveTrm` | 개월 수 |
| `taxType` | `GENERAL`(15.4%) / `NON_TAX`(0%) |

### 계산

기획 산식을 `BigDecimal`(정밀도 20자리)로 그대로 구현했고, 결과는 소수 둘째 자리 반올림이다.

| 계산기 | 단리 | 월복리 |
| --- | --- | --- |
| `DepositRateCalculator` | `P × r × n/12` | `P × (1 + r/12)^n − P` |
| `SavingRateCalculator` | `A × i × n(n+1)/2` | `A × (1+i) × ((1+i)^n − 1) / i − A × n` (금리 0%면 이자 0) |

응답: 원금(적금은 납입 원금 합계), 만기 금액, 세전 이자, 세율, 이자 과세, 세후 실수령액, 계산 가정 문구(적금만: "매월 초 동일액을 납입하는 것으로 가정한 계산이며...").

프리필(적용 금리 기본값 = 달성 가능 금리, 월 납입액 기본값 = 1단계 희망액)은 프론트가 상세 응답 값으로 채운다.

## 찜 (`/favorites`)

`RECOMMENDATION`, `ADMIN` 역할만 쓸 수 있다. 찜 단위는 **옵션**(`product_property_id`)이다.

| 기능 | 엔드포인트 | 비고 |
| --- | --- | --- |
| 추가 | `POST /favorites` | 20개 초과 409 `FAVORITE_LIMIT_EXCEEDED`, 중복 409 `FAVORITE_ALREADY_EXISTS`, 마감 옵션은 404 |
| 삭제 | `DELETE /favorites/{productPropertyId}` | |
| 여부 | `GET /favorites/{productPropertyId}/status` | |
| 개수 | `GET /favorites/count` | |
| 목록 (입력 기반 재계산) | `POST /favorites/list` | 본문은 추천 요청과 같은 `SearchRequestDto` |
| 목록 (기본) | `GET /favorites` | 입력 없이 조회. 지표(`metrics`)는 null |

목록은 찜 등록 최신순이다. `POST /favorites/list`는 요청 입력으로 적합도와 지표를 **조회 시점에 다시 계산**한다.

- 적합도의 거래 이력 반영 여부는 추천과 같은 `canUsePersonalization`으로 정한다. 지표(기여금 환산 수익률, 달성 가능 금리)는 이 판정과 관계없이 요청 입력으로 계산한다
- `#최고이율` 임계값은 찜한 상품만이 아니라 사용자의 가입 가능 은행 옵션 전체로 계산한다. 그래야 추천 리스트와 찜 카드의 적합도가 같다(이슈 #33, `MyFinFitScoreParityIntegrationTest`)
- 정부·은행 상품이 섞여 있으면 `showComparisonNotice = true` (카드 상단 "직접 비교할 수 없어요" 안내용)

## 추천 입력 프로필 (`/user/me/profile`)

Y5-2와 "입력 정보 저장 동의"의 저장소다. `RECOMMENDATION`, `ADMIN` 역할만 쓸 수 있다.

- `PUT`: 저장(upsert). 2단계 수치, 월 납입 희망액, 거래 이력 은행 코드, 1단계 선택 옵션 ID를 통째로 덮어쓴다. 저장 동의를 받았을 때만 호출하는 것은 프론트 책임이다.
- `GET`: 프리필용 저장값 + 표시용 계산값(만 나이, 가구원 수 기준 소득 가이드 금액, 지역명, 우대조건 라벨, 은행 코드 → 은행명). 저장값이 없으면 `hasProfile = false`.
- `DELETE`: 프로필 삭제 (저장 동의 철회). 없으면 아무 일도 하지 않는다.
