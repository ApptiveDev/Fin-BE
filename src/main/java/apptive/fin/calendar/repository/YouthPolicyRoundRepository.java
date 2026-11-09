package apptive.fin.calendar.repository;

import apptive.fin.calendar.entity.YouthPolicyRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface YouthPolicyRoundRepository extends JpaRepository<YouthPolicyRound, Long> {

    // 시작일이나 종료일이 기간 안에 있는 회차. 캘린더 일정은 이 두 날짜에만 찍힌다.
    @Query("""
            select r from YouthPolicyRound r join fetch r.policy p
            where p.active = true
              and p.applyPeriodType = apptive.fin.calendar.entity.ApplyPeriodType.PERIOD
              and (r.applyStartDate between :from and :to or r.applyEndDate between :from and :to)
            """)
    List<YouthPolicyRound> findActiveRoundsWithDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select r from YouthPolicyRound r join fetch r.policy p
            where p.active = true
              and p.applyPeriodType = apptive.fin.calendar.entity.ApplyPeriodType.PERIOD
              and r.applyStartDate <= :day and r.applyEndDate >= :day
            """)
    List<YouthPolicyRound> findActiveRoundsOpenOn(@Param("day") LocalDate day);
}
