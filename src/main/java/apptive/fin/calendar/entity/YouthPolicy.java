package apptive.fin.calendar.entity;

import apptive.fin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 온통청년 청년정책. 수집기(Fin-API)가 쓰고 이 앱은 캘린더 조회에만 읽는다. */
@Entity
@Table(name = "youth_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YouthPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", nullable = false, length = 50)
    private String policyNo;

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "institution", columnDefinition = "TEXT")
    private String institution;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "region_codes", columnDefinition = "TEXT")
    private String regionCodes;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_period_type", nullable = false, length = 20)
    private ApplyPeriodType applyPeriodType;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;
}
