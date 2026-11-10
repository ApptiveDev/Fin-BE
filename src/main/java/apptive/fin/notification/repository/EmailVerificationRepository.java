package apptive.fin.notification.repository;

import apptive.fin.notification.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByTokenHash(String tokenHash);

    @Query("SELECT ev FROM EmailVerification ev WHERE ev.user.id = :userId AND ev.email = :email AND ev.usedAt IS NULL ORDER BY ev.createdAt DESC LIMIT 1")
    Optional<EmailVerification> findLatestByUserIdAndEmail(@Param("userId") Long userId, @Param("email") String email);

    void deleteByUserId(Long userId);
}
