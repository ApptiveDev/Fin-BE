# 개발 가이드

## 준비물

- JDK 21
- Docker (로컬 DB, 통합 테스트의 Testcontainers)

## 로컬 실행

1. 저장소 루트에 `.env`를 만든다(git 미추적). 필요한 키는 [인증과 접근 제어](auth-and-access.md#환경-변수) 참고.

   ```properties
   JWT_SECRET=...
   GOOGLE_CLIENT_ID=...
   GOOGLE_CLIENT_SECRET=...
   KAKAO_CLIENT_ID=...
   KAKAO_CLIENT_SECRET=...
   ```

2. DB를 띄운다. `docker-compose.yml`은 PostgreSQL 16을 `localhost:5678`에 연다.

   ```bash
   docker compose up -d
   ```

   `spring-boot-docker-compose`가 개발 의존성에 있어 `bootRun` 때 자동으로 띄워 주기도 한다.

3. 스키마와 상품 시드를 넣는다.

   ```bash
   ./gradlew resetDb                     # 스키마 초기화 + Flyway + 시드 상품
   ./gradlew resetDb --args="--no-seed"  # 상품 시드 없이 스키마만
   ```

   `resetDb`는 `flyway.clean()`으로 DB를 통째로 비운 뒤 마이그레이션과 시드를 다시 적용한다. 접속 정보는 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` 환경 변수를 쓰고, 없으면 로컬 기본값(`localhost:5678`)을 쓴다. 이 변수에 운영 DB가 잡혀 있는 셸에서는 실행하지 않는다.

4. 서버를 띄운다. 기본 프로필은 `dev`다.

   ```bash
   ./gradlew bootRun
   ```

   Swagger UI: `http://localhost:8080/swagger-ui.html` (dev 프로필에서만). 프론트는 `http://localhost:5173`을 기준으로 CORS와 OAuth 리다이렉트가 설정되어 있다.

## 테스트

```bash
./gradlew test              # 단위 테스트
./gradlew integrationTest   # 통합 테스트 (Testcontainers, Docker 필요)
./gradlew check             # 둘 다
./gradlew codeHealthReports # JaCoCo·PMD·CPD 리포트 (build/reports/)
```

- `IntegrationTestSupport`를 상속한 테스트는 `integration` 태그가 붙어 `integrationTest`에서만 돈다.
- 테스트 설정은 `src/test/resources/application-test.yml`, 상품 픽스처는 `src/test/resources/sql/*.sql`.
- `FlywayMigrationIntegrationTest`가 마이그레이션 전체가 빈 DB에 적용되는지 확인한다.
- 추천 로직을 바꿀 때는 `PrdGoldenScenarioTest`(기획 문서 예시 시나리오)와 `GovernmentSeedYieldIntegrationTest`(시드 정부 상품 수익률)가 깨지는지 먼저 본다. 기대값을 바꿔야 한다면 기획 문서의 산식이 바뀐 것인지 확인한다.

## CI

| 워크플로 | 역할 | 머지 차단 |
| --- | --- | --- |
| `backend-ci.yml` | `test`, `integrationTest`. 모든 PR에서 돌고, 배포 전에 `deploy.yml`이 호출 | O |
| `deploy.yml` | `main` → dev 서버, `release` → 운영 서버 배포. [배포](deployment.md) 참고 | - |
| `backend-code-health.yml` | 커버리지·PMD·CPD·Code Health를 main 스냅샷과 비교해 PR 코멘트로 게시 | X |

자세한 내용은 루트 README의 CI 절.

## 작업 규칙

- **스키마 변경**: 새 `V{n}__설명.sql`을 추가한다. 적용된 마이그레이션은 고치지 않는다. 엔티티도 같이 바꾸고(`ddl-auto: validate`), 시드 SQL이 새 컬럼과 맞는지 확인한다.
- **키워드·카테고리 추가**: `KeywordValueEnum`, `CategoryIdEnum`, `category_option` 시드(마이그레이션)를 함께 바꾼다. 수집기가 같은 코드 문자열을 쓰므로 수집기 쪽에도 알린다.
- **추천 수치 일관성**: 리스트, 상세, 찜이 같은 값을 보여야 한다. 개인화 판정은 `SearchRequestPolicy`, 금리·수익률은 `RateCalculatorService`, 적합도는 `MatchScoreService` 한 곳에서만 계산하고 다른 곳에서 다시 구현하지 않는다. 자세한 내용은 [추천 엔진](recommendation-engine.md#일관성을-지키는-장치).
- **API 변경**: 저장소 루트 README의 API 명세도 같이 고친다.
- **기획과 다르게 구현할 때**: [기획과 구현 차이](plan-vs-implementation.md)에 이유와 함께 남긴다.
