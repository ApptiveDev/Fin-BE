package apptive.fin.term.repository;

import apptive.fin.term.entity.Term;
import apptive.fin.term.entity.UserTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import apptive.fin.user.entity.User;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserTermRepository extends  JpaRepository<UserTerm,Long> {
    @Query("SELECT ut FROM UserTerm ut JOIN FETCH ut.term WHERE ut.user = :user")
    List<UserTerm> findAllByUser(User user);

    Optional<UserTerm> findByUserAndTerm(User user, Term term);
}
