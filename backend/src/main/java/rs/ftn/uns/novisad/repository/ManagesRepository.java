package rs.ftn.uns.novisad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.model.Manages;

import java.util.List;
import java.util.Optional;

public interface ManagesRepository extends JpaRepository<Manages, Long> {

    List<Manages> findByLocationIdOrderByAssignedAtAsc(Long locationId);

    List<Manages> findByUserIdOrderByAssignedAtAsc(Long userId);

    Optional<Manages> findByUserIdAndLocationId(Long userId, Long locationId);

    boolean existsByUserIdAndLocationId(Long userId, Long locationId);

    long countByUserId(Long userId);

    /** Mesta kojima upravlja dati korisnik - koristi se na profilu [K10]. */
    @Query("select m.location from Manages m where m.user.id = :userId and m.location.active = true order by m.location.name")
    List<Location> findActiveLocationsManagedBy(Long userId);
}
