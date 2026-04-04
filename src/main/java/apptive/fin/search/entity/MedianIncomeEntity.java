package apptive.fin.search.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MedianIncomeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "household_size", nullable = false)
    private Integer householdSize;

    @Column(name = "earn_percent", nullable = false)
    private Integer earnPercent;

    @Column(name = "monthly_income", nullable = false)
    private Integer monthlyIncome;

    @Builder
    public MedianIncomeEntity(int year, int householdSize, int earnPercent, int monthlyIncome) {
        this.year = year;
        this.householdSize = householdSize;
        this.earnPercent = earnPercent;
        this.monthlyIncome = monthlyIncome;
    }


}
