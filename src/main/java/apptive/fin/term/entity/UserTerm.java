package apptive.fin.term.entity;

import apptive.fin.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_terms", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_id_term_id", columnNames = {"user_id", "term_id"})
    },
    indexes = {
        @Index(name = "idx_user_terms_term_id", columnList = "term_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Term term;

    @Column(name = "agreed", nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt = null;


    @Builder
    public UserTerm(User user, Term term, boolean agreed, LocalDateTime agreedAt) {
        this.user = user;
        this.term = term;
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }

    public void agree(){
        this.agreed = true;
        this.agreedAt = LocalDateTime.now();
    }
}
