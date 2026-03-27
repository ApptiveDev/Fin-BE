package apptive.fin.term.repository;

import apptive.fin.term.entity.TermVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermVersionRepository extends JpaRepository<TermVersion, Long> {
}
