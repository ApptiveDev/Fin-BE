package apptive.fin.notification.repository;

import apptive.fin.notification.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByTokenHash(String tokenHash);

    @Query("SELECT ev FROM EmailVerification ev WHERE ev.user.id = :userId AND ev.email = :email AND ev.usedAt IS NULL ORDER BY ev.createdAt DESC LIMIT 1")
    Optional<EmailVerification> findLatestByUserIdAndEmail(@Param("userId") Long userId, @Param("email") String email);

    @Query("SELECT ev FROM EmailVerification ev WHERE ev.usedAt IS NULL AND ev.expiresAt > :now AND ev.tokenPrefix = :prefix")
    List<EmailVerification> findValidByTokenPrefix(@Param("prefix") String prefix, @Param("now") Instant now);

    default List<EmailVerification> findValidByTokenPrefix(String prefix) {
        return findValidByTokenPrefix(prefix, Instant.now());
    }

    void deleteByUserId(Long userId);
}
