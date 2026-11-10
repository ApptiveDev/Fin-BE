package apptive.fin.notification.entity;

import apptive.fin.global.entity.BaseTimeEntity;
import apptive.fin.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "subscription_registration")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionRegistration extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "deposit_amount")
    private Long depositAmount;

    @Builder
    public SubscriptionRegistration(User user, Long productId, LocalDate startDate,
                                    int termMonths, LocalDate maturityDate, Long depositAmount) {
        this.user = user;
        this.productId = productId;
        this.startDate = startDate;
        this.termMonths = termMonths;
        this.maturityDate = maturityDate;
        this.depositAmount = depositAmount;
    }
}
