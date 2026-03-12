package apptive.fin.term;

import org.springframework.data.jpa.repository.JpaRepository;
import apptive.fin.user.User;
import java.util.List;
import java.util.Optional;

public interface UserTermRepository extends  JpaRepository<UserTerm,Long> {
    List<UserTerm> findAllByUser(User user);
    Optional<UserTerm> findByUserAndTerm(User user,Term term);
}
