# 작업 안내

Y-Fin.(청년 금융상품 추천 서비스)의 API 서버다. Spring Boot 4, Java 21, 루트 패키지 `apptive.fin`. 자세한 설계와 규칙은 [docs/README.md](docs/README.md)에서 시작한다.

## 관련 저장소

- 수집기 [ApptiveDev/Fin-API](https://github.com/ApptiveDev/Fin-API): 공공 API와 수동 상품 데이터를 수집, 정규화(필요하면 Gemini로 보강)해 이 서버가 읽는 상품 테이블에 넣는다. 같은 PostgreSQL을 쓰고, 수집기용 테이블 스키마도 이 저장소의 Flyway 이력으로 관리한다.
- 프론트 [ApptiveDev/Fin-FE](https://github.com/ApptiveDev/Fin-FE): React + Vite.
- API 명세 [Fin API 명세](https://yuyeol3.github.io/fin-api-docs/): 원문 마크다운은 [yuyeol3/fin-api-docs](https://github.com/yuyeol3/fin-api-docs).

## 빌드, 테스트, 실행

Windows에서는 `./gradlew.bat`을 쓴다. 자세한 내용은 [개발 가이드](docs/implementation/development.md).

```bash
docker compose up -d                  # 로컬 DB (pgvector/pgvector:pg16, localhost:5678, user/password, DB 이름 database)
./gradlew resetDb                     # Flyway clean + migrate 후 상품 시드 적재
./gradlew resetDb --args="--no-seed"  # 스키마와 기준 데이터만
./gradlew bootRun                     # 서버 실행 (기본 프로필 dev)
./gradlew test                        # 단위 테스트
./gradlew integrationTest             # 통합 테스트 (Testcontainers, Docker 필요)
./gradlew test --tests "apptive.fin.search.MatchScoreServiceTest"           # 클래스 하나
./gradlew test --tests "*.MatchScoreServiceTest.someMethod"                 # 메서드 하나
```

- 비밀 값은 git에 올리지 않는 루트 `.env`에서 읽는다(`spring.config.import: optional:file:.env[.properties]`). OAuth 클라이언트 ID와 시크릿, `JWT_SECRET`이 필요하다.
- `docker-compose.yml`에는 볼륨이 없어서 컨테이너를 지우면 데이터도 사라진다.
- `resetDb`는 DB를 통째로 비운다. 로컬 개발 DB에서만 쓰고, `SPRING_DATASOURCE_*`에 원격 DB가 잡힌 셸에서는 실행하지 않는다.
- Swagger UI와 `/v3/api-docs`는 `dev`, `test` 프로필에서만 열린다.

## 스키마 (Flyway)

- JPA는 `ddl-auto: validate`이고, 스키마의 원본은 `src/main/resources/db/migration/`의 Flyway 이력이다.
- 스키마나 기준 데이터를 바꿀 때는 새 `V<버전>__<설명>.sql`을 추가한다. 이미 적용된 마이그레이션은 고치지 않는다. 엔티티도 함께 바꾸고, 시드 SQL(`src/main/resources/seed/*.sql`)이 새 컬럼과 맞는지 확인한다.
- `baseline-on-migrate`의 기준 버전은 2다. Flyway 도입 전 DB는 V2(레거시 스키마와 기준 데이터)로 기록되고 V3부터 적용된다.
- 기준 데이터(중위소득, 약관, 카테고리)는 마이그레이션에, 개발용 상품 데이터는 시드에 있다.

## 구조

기능별 패키지(`auth`, `user`, `term`, `category`, `search`, `calculator`, `myfin`(찜), `provider`, `calendar`)와 공통 `global`로 나누고, 각 패키지는 `controller / service / dto / entity / repository`로 나눈다. 자세한 내용은 [아키텍처](docs/implementation/architecture.md).

- **인증**: OAuth2(구글, 카카오) 로그인 후 JWT 액세스 토큰과 리프레시 토큰을 발급한다. `SecurityConfig`는 세션을 쓰지 않고, `JwtAuthFilter`가 토큰을 읽는다. 공개 경로와 역할(`BEFORE_AGREED`, `RECOMMENDATION`, `ADMIN`)은 `SecurityConfig`와 [인증과 접근 제어](docs/implementation/auth-and-access.md)를 본다. CORS 허용 출처는 `app.frontend.urls`(쉼표로 여러 개)다.
- **추천**: `SearchService`가 `ResolveKeywordService`(요청 옵션 → 키워드), `EligibilityFilterService`(가입 자격), `MatchScoreService`(적합도), `RateCalculatorService`(달성 가능 금리, 수익률)를 차례로 호출한다. 리스트, 상세, 찜이 같은 값을 보여야 하므로 개인화 판정은 `SearchRequestPolicy`, 금리와 수익률은 `RateCalculatorService`, 적합도는 `MatchScoreService` 한 곳에서만 계산한다. 자세한 내용은 [추천 엔진](docs/implementation/recommendation-engine.md).
- **오류**: 도메인마다 `global/error/ErrorCode`를 구현한 enum(`SearchErrorCode`, `AuthErrorCode` 등, 코드는 `접두사 + 번호`)을 두고 `BusinessException(errorCode)`를 던진다. `GlobalExceptionHandler`가 `ErrorResponseDto`로 바꾼다. 새 실패 상황은 임의의 예외 대신 enum 상수로 추가한다.

## 테스트

- `IntegrationTestSupport`를 상속한 테스트는 `integration` 태그가 붙어 `integrationTest`에서만 돈다. `@SpringBootTest`, 프로필 `dev`+`test`, Testcontainers PostgreSQL을 쓰므로 로컬 DB를 건드리지 않는다.
- 테스트 설정은 `src/test/resources/application-test.yml`(`dev` 위에 테스트용 값만 덮어씀. 스키마는 Flyway로 초기화하고 쿼리 수 회귀 테스트용 Hibernate 통계를 켬), SQL 픽스처는 `src/test/resources/sql/`.
- 순수 로직 서비스는 Spring 컨텍스트 없는 JUnit 단위 테스트로 검증한다.

## API 명세 갱신

API 명세는 이 저장소의 코드와 자동으로 맞춰지지 않는다. API 계약(경로, 요청과 응답 필드, 인증 수준, 오류 코드, 동작 규칙)을 바꾸는 PR은 본문에 영향을 받는 명세 문서를 적고, 머지 후 fin-api-docs의 해당 `docs/*.md`와 `README.md`의 기준 커밋을 갱신한다. 형식과 Swagger(`/v3/api-docs`) 대조 절차는 [fin-api-docs의 AGENTS.md](https://github.com/yuyeol3/fin-api-docs/blob/main/AGENTS.md)를 따른다.

## 컨벤션

- Java 21, Lombok, DTO와 설정은 record, 기능별 패키지.
- 주석, 커밋 메시지, 사용자에게 보이는 오류 메시지는 한국어로 쓴다. 주변 스타일에 맞춘다.
- 커밋 메시지는 `type : 한국어 설명` 형식이다(예: `feat : B1 캘린더 일정, 요약 조회 API 추가`, `fix : ...`, `docs : ...`). PR은 squash 머지한다.
- 기획과 다르게 구현했다면 [기획과 구현 차이](docs/implementation/plan-vs-implementation.md)에 이유와 함께 남긴다.
