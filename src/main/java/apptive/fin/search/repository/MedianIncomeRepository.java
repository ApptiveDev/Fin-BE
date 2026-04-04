package apptive.fin.search.repository;


import apptive.fin.search.dto.MedianIncomesDto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedianIncomeRepository extends JpaRepository<MedianIncomesDto, Long> {
}
