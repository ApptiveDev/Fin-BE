package apptive.fin.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// 통합 테스트 공통 베이스. Testcontainers Postgres로 격리되며 dev DB를 건드리지 않는다.
// dev+test 프로파일: DB 관련 키만 test가 덮고(뒤 프로파일 우선), 나머지 부팅 설정은 dev에서 상속.
// integration 태그: 상속한 테스트는 test가 아닌 integrationTest 태스크에서 실행된다.
@Tag("integration")
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@Import(PostgresTestContainerConfig.class)
public abstract class IntegrationTestSupport {
}
