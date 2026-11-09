package apptive.fin.auth;

import apptive.fin.auth.dto.LoginResponseDto;
import apptive.fin.auth.service.AuthService;
import apptive.fin.global.error.BusinessException;
import apptive.fin.support.IntegrationTestSupport;
import apptive.fin.user.UserRole;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 같은 리프레시 토큰으로 회전 요청이 겹칠 때(#9의 500 오류 상황)를 실제 Postgres에서 재현한다.
 * 타이밍에 기대지 않도록 순서를 고정한다: A가 회전을 마치고 커밋 전 대기 → B가 아직 활성인 토큰을 읽고
 * 같은 행 잠금에서 대기 → A 커밋. B는 500이 아니라 401(UNAUTHORIZED)이어야 한다.
 */
class RefreshTokenConcurrencyIntegrationTest extends IntegrationTestSupport {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void 같은_토큰으로_회전_요청이_겹치면_하나만_성공하고_나머지는_401이다() throws Exception {
        User user = createUser();
        String token = authService.getRefreshToken(user);
        CountDownLatch firstRotated = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<LoginResponseDto> first = executor.submit(() -> transaction.execute(status -> {
                LoginResponseDto response = authService.refresh(token);
                firstRotated.countDown();
                awaitQuietly(releaseFirst);
                return response;
            }));
            assertThat(firstRotated.await(10, SECONDS)).isTrue();

            Future<LoginResponseDto> second = executor.submit(() -> authService.refresh(token));
            awaitSomeoneWaitingOnRowLock();
            releaseFirst.countDown();

            assertThat(first.get(10, SECONDS).accessToken()).isNotBlank();
            assertUnauthorized(second);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }

        // 회전에 쓴 토큰은 이후에도 다시 쓸 수 없다
        assertThatThrownBy(() -> authService.refresh(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.UNAUTHORIZED);

        // 쓴 토큰 행은 지워지고 A가 새로 받은 토큰 하나만 남는다
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where user_id = ?", Integer.class, user.getId()))
                .isEqualTo(1);
    }

    private void assertUnauthorized(Future<?> future) {
        assertThatThrownBy(() -> future.get(10, SECONDS))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.UNAUTHORIZED);
    }

    private void awaitSomeoneWaitingOnRowLock() throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbcTemplate.queryForObject(
                    "select count(*) from pg_stat_activity "
                            + "where datname = current_database() and wait_event_type = 'Lock'",
                    Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("두 번째 요청이 행 잠금에서 대기하지 않았다");
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .name("refresh-tester")
                .email("refresh-tester@example.com")
                .provider("google")
                .providerId("pid-" + System.nanoTime())
                .userRole(UserRole.BEFORE_AGREED)
                .build());
    }
}
