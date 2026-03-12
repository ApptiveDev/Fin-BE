package apptive.fin.term.repository;

import apptive.fin.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term,Long> {
}
