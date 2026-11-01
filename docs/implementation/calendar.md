# 캘린더 (B1)

> 기획: [B1 정부상품 캘린더](../planning/b1-government-calendar.md)

온통청년 청년정책의 신청 일정을 보여준다. 기획의 2계층(청년 금융 지원 정책)만 구현했고, 1계층(Y-Fin 정책상품 16종)은 아직 넣지 않았다. 1계층은 `manual-products.json`으로 수동 입력하는 상품이라 회차 날짜가 없다.

## 데이터

수집기(Fin-API)가 온통청년 `getPlcy` API에서 정책을 받아 `youth_policy`, `youth_policy_round`(V10)에 쓴다. 추천 대상인 `product`와는 테이블을 나눴다.

| 항목 | 규칙 |
| --- | --- |
| 범위 | 온통청년 중분류 `취약계층 및 금융지원`인 정책. 수집기 `YouthPolicyParser.FINANCE_CATEGORY` |
| 신청기간 구분 | `aplyPrdSeCd` 0057001 특정기간 → `PERIOD`, 0057002 상시 → `ALWAYS`, 0057003 마감 → `CLOSED`(표시 안 함) |
| 회차 | `aplyYmd`의 `yyyyMMdd ~ yyyyMMdd` 하나가 한 회차. 여러 회차는 문자 그대로의 `\N`으로 이어져 온다. 읽을 수 없는 회차는 버린다 |
| 일정 변경 | 같은 순번(`round_no`) 회차의 날짜가 바뀌면 `schedule_changed_at`을 남기고, API는 7일간 `scheduleChanged = true` |
| 사라진 정책 | 이번 수집 목록에 없으면 `is_active = false` |
| 나이 | 온통청년은 제한 없음을 0으로 준다. 0은 `NULL`로 저장 |
| 지역 | `zipCd`(시군구 5자리 코드) 원문을 `region_codes`에 저장. 비어 있으면 지역 제한 없음 |

## API

두 엔드포인트 모두 비로그인으로 쓸 수 있다. 신청 기간 유형, 상품 분류, 내 조건 맞춤 필터는 FE가 응답 필드(`type`, `eligibility`)로 거른다. 필터 칩마다 건수를 보여줘야 해서 서버가 거르지 않는다.

| API | 내용 |
| --- | --- |
| `GET /calendar/events?from=yyyy-MM-dd&to=yyyy-MM-dd` | 기간(양 끝 포함)에 찍히는 시작·마감 일정. 날짜순, 같은 날은 마감 먼저 |
| `GET /calendar/summary` | 오늘 기준 정보: `openNow`(지금 신청 가능, 마감 가까운 순), `yearRound`(접수 중인 연중 접수), `always`(상시 신청) |

`from`이 `to`보다 늦거나 날짜 형식이 틀리면 400이다. `asOf`는 마지막 수집 시각으로, 하단 고지의 데이터 기준일에 쓴다.

## 일정 판정

`CalendarSchedule`이 기획의 판정 규칙을 그대로 옮겼다.

- 연중 접수: 기간이 양 끝 포함 180일 이상이고 12/31에 끝나는 회차. 마감 일정이 없고, 시작일이 미래면 시작 일정만 찍는다.
- 그 외 회차: 시작·마감 일정 두 개. 시작일과 종료일이 같으면 마감 일정만.
- 지금 신청 가능: 오늘이 시작일~종료일 사이(양 끝 포함)인 회차. 연중 접수는 `yearRound`로 따로 준다.

## 자격 판정

`PolicyEligibility`. 기획대로 2계층은 나이와 거주지역만 본다. 약관 동의를 마친 로그인 사용자에게만 판정하고, 그 외에는 `eligibility = null`이다.

- 저장된 추천 입력 프로필(`user_profiles`)의 생년월일과 1단계 거주지역(`REGION_*`)을 쓴다.
- 정책에 제한이 있는데 프로필 값이 비면 `NEEDS_INFO`. 다른 조건에서 이미 미충족이면 `INELIGIBLE`이 우선한다.
- 거주지역은 `zipCd` 앞 두 자리(시도 코드)로 맞춘다. 2026 행정구역 개편으로 광주·전남은 `12`(전남광주통합특별시), 강원은 `51`, 전북은 `52`를 쓴다. 모든 시도가 대상이면 전국으로 보고 `regions`를 비운다.
