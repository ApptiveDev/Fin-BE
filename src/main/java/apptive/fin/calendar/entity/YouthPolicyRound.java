package apptive.fin.calendar.entity;

import apptive.fin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/** 청년정책의 신청 회차. 온통청년 aplyYmd의 "시작 ~ 종료" 하나. */
@Entity
@Table(name = "youth_policy_round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YouthPolicyRound extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private YouthPolicy policy;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "apply_start_date", nullable = false)
    private LocalDate applyStartDate;

    @Column(name = "apply_end_date", nullable = false)
    private LocalDate applyEndDate;

    @Column(name = "schedule_changed_at")
    private Instant scheduleChangedAt;
}
