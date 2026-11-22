package apptive.fin.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// Caffeine이 클래스패스에 있으면 Spring Boot가 CaffeineCacheManager를 쓴다.
// 따로 등록하지 않은 캐시(keywordOptionMap 등)는 기존처럼 만료 없이 유지된다.
@Configuration
public class CacheConfig {

    // 상품 상세의 #최고이율 기준 금리. 목록 카드마다 같은 조건으로 상세를 불러서 조건별로 잠깐 재사용한다(#52).
    // 상품 데이터는 수집기가 하루 단위로 갱신하므로 10분 전 값을 써도 영향이 작다.
    public static final String BANK_MAX_INTEREST_THRESHOLD = "bankMaxInterestThreshold";

    @Bean
    CacheManagerCustomizer<CaffeineCacheManager> bankMaxInterestThresholdCache() {
        return cacheManager -> cacheManager.registerCustomCache(
                BANK_MAX_INTEREST_THRESHOLD,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .maximumSize(1_000)
                        .build()
        );
    }
}
