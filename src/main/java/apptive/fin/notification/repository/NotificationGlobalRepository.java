package apptive.fin.notification.repository;

import apptive.fin.notification.entity.NotificationGlobal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationGlobalRepository extends JpaRepository<NotificationGlobal, Long> {

    Optional<NotificationGlobal> findByEmail(String email);

    boolean existsByEmailAndEnabledFalse(String email);
}
