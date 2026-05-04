package apptive.fin.search.service;

import apptive.fin.search.dto.SearchRequestDto;
import apptive.fin.search.entity.Product;
import apptive.fin.search.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EligibilityFilterService {

    private final ProductRepository productRepository;

    public List<Product> filterEligible(SearchRequestDto request){
        var detail = request.detailedOptions();

        int age = Period.between(detail.birthdate(), LocalDate.now()).getYears();

        Integer tenureMonths = Boolean.TRUE.equals(detail.isFirstJob())? null : detail.tenureMonths();

        return productRepository.findEligibleProducts(
                age, detail.annualIncome(), detail.isHomeless(),
                detail.isHouseholder(),tenureMonths, detail.monthlySavingsGoal()
        );

    }
}
