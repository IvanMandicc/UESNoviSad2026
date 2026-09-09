package rs.ftn.uns.novisad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ftn.uns.novisad.model.Location;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByActiveTrueOrderByNameAsc();

    Optional<Location> findByIdAndActiveTrue(Long id);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndActiveTrue(String name, Long id);
}
