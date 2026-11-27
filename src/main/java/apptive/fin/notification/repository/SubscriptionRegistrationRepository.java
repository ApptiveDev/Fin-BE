package apptive.fin.notification.repository;

import apptive.fin.notification.entity.SubscriptionRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRegistrationRepository extends JpaRepository<SubscriptionRegistration, Long> {

    List<SubscriptionRegistration> findByUserId(Long userId);

    List<SubscriptionRegistration> findByMaturityDateBetween(LocalDate start, LocalDate end);
}
