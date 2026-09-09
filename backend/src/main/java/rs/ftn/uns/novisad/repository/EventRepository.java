package rs.ftn.uns.novisad.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rs.ftn.uns.novisad.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Mesto se svuda dovlaci preko {@code @EntityGraph} jer se dogadjaj mapira u DTO
 * tek u kontroleru, van transakcije (spring.jpa.open-in-view = false).
 */
public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    @EntityGraph(attributePaths = "location")
    Optional<Event> findByIdAndActiveTrue(Long id);

    /** [K3] Predstojeci dogadjaji na stranici mesta. */
    @EntityGraph(attributePaths = "location")
    List<Event> findByLocationIdAndActiveTrueAndDateAfterOrderByDateAsc(Long locationId, LocalDateTime from);

    /** Svi dogadjaji mesta, najnoviji prvi - za menadzera. */
    @EntityGraph(attributePaths = "location")
    List<Event> findByLocationIdAndActiveTrueOrderByDateDesc(Long locationId);

    /** [K8] Danasnji dogadjaji sa svih mesta. */
    @EntityGraph(attributePaths = "location")
    List<Event> findByActiveTrueAndDateBetweenOrderByDateAsc(LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = "location")
    List<Event> findByActiveTrueOrderByDateAsc();

    /**
     * [K5] Koliko puta se dogadjaj sa datim nazivom vec odrzao na datom mestu.
     * Redovan dogadjaj se u bazi vodi kao vise pojava sa istim nazivom i razlicitim
     * datumima, pa je broj odrzavanja broj pojava u proslosti.
     */
    long countByNameIgnoreCaseAndLocationIdAndActiveTrueAndDateBefore(String name,
                                                                     Long locationId,
                                                                     LocalDateTime before);

}
