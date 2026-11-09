package apptive.fin.auth.service;

import apptive.fin.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// 로그아웃 없이 떠난 세션의 토큰은 회전으로 지워지지 않으므로 만료되면 일괄 삭제한다.
// 같은 삭제를 여러 번 실행해도 결과가 같아 인스턴스가 여럿이어도 락이 필요 없다.
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpired(Instant.now());
        log.info("만료된 리프레시 토큰 {}건 삭제", deleted);
    }
}
