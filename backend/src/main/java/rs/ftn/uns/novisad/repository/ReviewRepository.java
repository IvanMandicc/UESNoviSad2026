package rs.ftn.uns.novisad.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ftn.uns.novisad.model.Review;

import java.util.List;
import java.util.Optional;

/**
 * Povezani entiteti se dovlace preko {@code @EntityGraph} jer se utisak mapira
 * u DTO tek u kontroleru, van transakcije (spring.jpa.open-in-view = false).
 * <p>
 * Uklonjeni utisci (active = false) se nigde ne racunaju; sakriveni (hidden = true)
 * se ne prikazuju, ali njihove ocene ulaze u prosek mesta - kako trazi [M2].
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"location", "event", "author", "rates"})
    Optional<Review> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = {"location", "event", "author", "rates"})
    List<Review> findByLocationIdAndActiveTrueAndHiddenFalseOrderByCreatedAtDesc(Long locationId);

    /** [M2] Menadzer i administrator vide i sakrivene utiske. */
    @EntityGraph(attributePaths = {"location", "event", "author", "rates"})
    List<Review> findByLocationIdAndActiveTrueOrderByCreatedAtDesc(Long locationId);

    /** [K10] Utisci koje je korisnik ostavio - za prikaz na profilu. */
    @EntityGraph(attributePaths = {"location", "event", "author", "rates"})
    List<Review> findByAuthorIdAndActiveTrueOrderByCreatedAtDesc(Long authorId);

    boolean existsByAuthorIdAndEventIdAndActiveTrue(Long authorId, Long eventId);

    long countByLocationIdAndActiveTrue(Long locationId);

    /** [K3] Ukupna, srednja vrednost ocene mesta - prosek svih ocena svih stavki. */
    @Query("""
            select avg(rate.value)
            from Rate rate
            where rate.review.location.id = :locationId
              and rate.review.active = true
            """)
    Double findAverageRating(@Param("locationId") Long locationId);

    /** Prosek po pojedinacnoj stavci; koristi se i za UES pretragu [S1]. */
    @Query("""
            select rate.category, avg(rate.value)
            from Rate rate
            where rate.review.location.id = :locationId
              and rate.review.active = true
            group by rate.category
            """)
    List<Object[]> findAverageRatingByCategory(@Param("locationId") Long locationId);

    /** Prosecne ocene za vise mesta odjednom - da lista mesta ne pravi upit po mestu. */
    @Query("""
            select rate.review.location.id, avg(rate.value)
            from Rate rate
            where rate.review.active = true
            group by rate.review.location.id
            """)
    List<Object[]> findAverageRatingForAllLocations();

    @Query("""
            select review.location.id, count(review)
            from Review review
            where review.active = true
            group by review.location.id
            """)
    List<Object[]> countReviewsForAllLocations();
}
