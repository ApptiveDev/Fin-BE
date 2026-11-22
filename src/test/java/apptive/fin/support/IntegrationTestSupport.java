package apptive.fin.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
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

    @Autowired
    private CacheManager cacheManager;

    // 캐시는 스프링 컨텍스트와 함께 테스트 사이에 공유된다. 테스트마다 픽스처 데이터가 다르므로 비우고 시작한다.
    @BeforeEach
    protected void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }
}
