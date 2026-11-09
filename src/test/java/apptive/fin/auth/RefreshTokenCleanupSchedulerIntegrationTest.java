package apptive.fin.auth;

import apptive.fin.auth.entity.RefreshToken;
import apptive.fin.auth.repository.RefreshTokenRepository;
import apptive.fin.auth.service.RefreshTokenCleanupScheduler;
import apptive.fin.support.IntegrationTestSupport;
import apptive.fin.user.UserRole;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCleanupSchedulerIntegrationTest extends IntegrationTestSupport {

    @Autowired private RefreshTokenCleanupScheduler scheduler;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ScheduledTaskHolder scheduledTaskHolder;

    @Test
    void 만료된_토큰만_삭제하고_유효한_토큰은_남긴다() {
        User user = createUser();
        String expiredHash = "expired-" + System.nanoTime();
        String validHash = "valid-" + System.nanoTime();
        saveToken(user, expiredHash, Instant.now().minusSeconds(60));
        saveToken(user, validHash, Instant.now().plusSeconds(3600));

        scheduler.deleteExpiredTokens();

        assertThat(refreshTokenRepository.findByTokenHash(expiredHash)).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(validHash)).isPresent();
    }

    @Test
    void 만료_토큰_정리는_매일_새벽_4시_작업으로_등록된다() {
        assertThat(scheduledTaskHolder.getScheduledTasks())
                .extracting(ScheduledTask::getTask)
                .filteredOn(task -> task.toString().endsWith("RefreshTokenCleanupScheduler.deleteExpiredTokens"))
                .singleElement()
                .isInstanceOfSatisfying(CronTask.class,
                        task -> assertThat(task.getExpression()).isEqualTo("0 0 4 * * *"));
    }

    private void saveToken(User user, String tokenHash, Instant expiresAt) {
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .user(user)
                .build());
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .name("cleanup-tester")
                .email("cleanup-tester@example.com")
                .provider("google")
                .providerId("pid-" + System.nanoTime())
                .userRole(UserRole.BEFORE_AGREED)
                .build());
    }
}
