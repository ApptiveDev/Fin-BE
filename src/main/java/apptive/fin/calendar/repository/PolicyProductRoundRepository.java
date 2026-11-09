package apptive.fin.calendar.repository;

import apptive.fin.calendar.entity.PolicyProductRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

// 가입 가능한 옵션이 없는 상품(수집에서 사라져 비활성화된 상품)의 회차는 보여주지 않는다.
// 캘린더 카드에 옵션의 제공기관을 쓰므로 옵션과 제공기관을 함께 읽는다. 따로 두면 제공기관마다 쿼리가 하나씩 나간다.
// 옵션의 키워드 컬렉션은 @BatchSize로 묶여 읽힌다.
public interface PolicyProductRoundRepository extends JpaRepository<PolicyProductRound, Long> {

    @Query("""
            select distinct r from PolicyProductRound r
              join fetch r.product p
              left join fetch p.properties property
              left join fetch property.provider
            where exists (select 1 from ProductProperty pp where pp.product = p and pp.isJoinable = true)
              and (r.applyStartDate between :from and :to or r.applyEndDate between :from and :to)
            """)
    List<PolicyProductRound> findRoundsWithDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select distinct r from PolicyProductRound r
              join fetch r.product p
              left join fetch p.properties property
              left join fetch property.provider
            where exists (select 1 from ProductProperty pp where pp.product = p and pp.isJoinable = true)
              and r.applyStartDate <= :day and r.applyEndDate >= :day
            """)
    List<PolicyProductRound> findRoundsOpenOn(@Param("day") LocalDate day);
}
