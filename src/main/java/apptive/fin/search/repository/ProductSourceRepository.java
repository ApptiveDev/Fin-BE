package apptive.fin.search.repository;

import apptive.fin.search.entity.ProductSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductSourceRepository extends JpaRepository<ProductSource, Integer> {
    Optional<ProductSource> findByCode(String code);
}
