package apptive.fin.calendar.repository;

import apptive.fin.calendar.entity.ApplyPeriodType;
import apptive.fin.calendar.entity.YouthPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface YouthPolicyRepository extends JpaRepository<YouthPolicy, Long> {

    List<YouthPolicy> findByActiveTrueAndApplyPeriodType(ApplyPeriodType applyPeriodType);

    @Query("select max(p.syncedAt) from YouthPolicy p")
    Instant findLastSyncedAt();
}
