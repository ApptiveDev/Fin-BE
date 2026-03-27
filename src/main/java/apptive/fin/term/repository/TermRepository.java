package apptive.fin.term.repository;

import apptive.fin.term.dto.TermResponseDto;
import apptive.fin.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TermRepository extends JpaRepository<Term,Long> {
    @Query("""
        SELECT NEW apptive.fin.term.dto.TermResponseDto(
            t.id,
            tv.id,
            t.code,
            tv.title,
            tv.content,
            tv.effectiveFrom,
            t.isRequired,
            CASE WHEN uta.id IS NOT NULL AND uta.agreed = true
                THEN true
                ELSE false
            END
        )
        FROM Term t
        LEFT JOIN TermVersion tv ON tv.term = t
        LEFT JOIN UserTermAgreement uta on uta.termVersion = tv AND uta.user.id = :userId
        WHERE tv.isCurrent = true
    """)
    List<TermResponseDto> getTermResponseDtosByUserId(Long userId);
}
