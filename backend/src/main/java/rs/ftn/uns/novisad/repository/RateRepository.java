package rs.ftn.uns.novisad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ftn.uns.novisad.model.Rate;

public interface RateRepository extends JpaRepository<Rate, Long> {
}
