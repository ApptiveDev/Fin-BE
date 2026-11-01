package apptive.fin.calendar.entity;

import apptive.fin.global.entity.BaseTimeEntity;
import apptive.fin.search.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

/** 1계층(Y-Fin 정책상품)의 신청 회차. 수집기가 manual-product-rounds.json에서 채운다. */
@Entity
@Table(name = "policy_product_round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyProductRound extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "apply_start_date", nullable = false)
    private LocalDate applyStartDate;

    @Column(name = "apply_end_date", nullable = false)
    private LocalDate applyEndDate;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "schedule_changed_at")
    private Instant scheduleChangedAt;
}
