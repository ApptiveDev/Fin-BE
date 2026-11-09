package apptive.fin.auth.repository;

import apptive.fin.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 엔티티 삭제(delete(entity), 파생 deleteBy...)는 동시 요청에서 0행이면 Hibernate가 예외를 던진다(#9의 500).
    // 벌크 삭제로 지운 행 수를 받아 호출한 쪽이 판단한다.
    @Modifying
    @Query("delete from RefreshToken r where r.tokenHash = :tokenHash")
    int deleteByTokenHashIfExists(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :now")
    int deleteExpired(Instant now);
}
