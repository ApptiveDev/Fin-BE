# Y-Fin. 백엔드 문서

Y-Fin. 기획(노션)과 이 저장소의 백엔드 구현을 정리한 문서다. API 엔드포인트 목록은 저장소 루트 [README](../README.md), 요청·응답 스키마는 dev 프로필의 Swagger UI를 본다.

## 처음 읽는 순서

1. [서비스 개요](planning/service-overview.md): 무엇을 왜 만드는지
2. [기능 맵](planning/feature-map.md): 기능 ID, 상태, 로그인별 허용 범위
3. [아키텍처](implementation/architecture.md): 시스템 구성과 패키지 구조
4. [추천 엔진](implementation/recommendation-engine.md): 서비스의 핵심 로직
5. [개발 가이드](implementation/development.md): 로컬 실행, 테스트, 작업 규칙

## 기획 (`planning/`)

노션 기획 문서를 개발 참고용으로 정리했다. 원본의 템플릿 문구와 중복은 덜어냈고, 규칙·수치·결정은 그대로 옮겼다. 각 문서 상단에 원본 노션 문서 이름을 적었다.

| 문서 | 내용 | 상태 |
| --- | --- | --- |
| [service-overview](planning/service-overview.md) | 문제 정의, 3단계 추천, 원칙, 로드맵 | |
| [feature-map](planning/feature-map.md) | 기능 목록, 사용자 흐름, 로그인 상태별 허용 범위 | |
| [y1-auth-and-terms](planning/y1-auth-and-terms.md) | SNS 로그인, 약관, 입력 정보 저장 동의, 개인정보 Q&A | 구현 |
| [y2-landing-and-explore](planning/y2-landing-and-explore.md) | 소개 페이지, 비로그인 탐색 (FE 중심) | 구현 |
| [y3-1-input](planning/y3-1-input.md) | 1·2단계 입력 항목, 동적 폼, 우대조건 체계, 은행 목록, 중위소득 | 구현 |
| [y3-2-recommendation-list](planning/y3-2-recommendation-list.md) | 자격 필터, 탭 A 배점, 탭 B 수익률·금리 산식, 정부 상품 기준값 | 구현 |
| [y4-product-detail](planning/y4-product-detail.md) | 상품 상세, 수익률 계산기, 아웃링크 | 구현 |
| [y5-mypage](planning/y5-mypage.md) | 찜해둔 Fin., 개인정보 | 구현 |
| [a1-a3-product-expansion](planning/a1-a3-product-expansion.md) | 정보입력 확장, 파킹통장, 단기 예적금 | 구현 중 (PR #36, #37) |
| [b1-government-calendar](planning/b1-government-calendar.md) | 정부상품 캘린더 | 기획 |
| [b2-notification](planning/b2-notification.md) | 마감·시작·만기 알림, 내 가입 상품 | 기획 |

## 구현 (`implementation/`)

`main` 브랜치 코드(#35까지) 기준이다. 열린 PR은 반영하지 않았다.

| 진행 중 PR | 내용 | 머지 후 갱신할 문서 |
| --- | --- | --- |
| #36 (draft) | A1~A3 이관: 대분류 라우팅, 세후 실수령액 정렬, 파킹통장 탭, V2·TOP3 배점, V7 | recommendation-engine, data-model, plan-vs-implementation, a1-a3-product-expansion |
| #37 (draft, #36 위) | 파킹 카드 필드 V8 매핑, 단기예치 상품 수 집계, 정렬 테스트 정리 | detail-calculator-favorites(파킹 카드), data-model |

| 문서 | 내용 |
| --- | --- |
| [architecture](implementation/architecture.md) | 시스템 구성, 스택, 패키지 책임, 요청 흐름, 에러 처리 |
| [auth-and-access](implementation/auth-and-access.md) | OAuth2 로그인, JWT·리프레시 토큰, 역할, 엔드포인트 접근 규칙, 약관, 환경 변수 |
| [recommendation-engine](implementation/recommendation-engine.md) | 입력 해소, 자격 필터, 적합도, 수익률·달성 가능 금리, 응답 구조 |
| [detail-calculator-favorites](implementation/detail-calculator-favorites.md) | 상품 상세와 수치 잠금, 신청 링크, 계산기, 찜, 추천 입력 프로필 |
| [data-model](implementation/data-model.md) | 마이그레이션 이력, 테이블과 컬럼 의미, 시드 데이터 |
| [development](implementation/development.md) | 로컬 실행, DB 초기화, 테스트, 작업 규칙 |
| [plan-vs-implementation](implementation/plan-vs-implementation.md) | 기획과 구현이 다른 부분, 루트 README와 코드 불일치 |

## 문서 관리

- 기획이 바뀌면 노션이 원본이다. 이 폴더의 기획 문서는 개발에 영향을 주는 변경이 있을 때 갱신한다.
- 구현 문서는 코드와 같은 PR에서 고친다.
- 기획과 다르게 구현했다면 [plan-vs-implementation](implementation/plan-vs-implementation.md)에 남긴다.
- 노션 export에 있던 발표 대본, 해커톤·공모전 제출 자료, 설문 홍보 문구, 인터뷰 스크립트는 개발과 무관해 옮기지 않았다.
