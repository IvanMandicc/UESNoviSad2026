package rs.ftn.uns.novisad.repository;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import rs.ftn.uns.novisad.model.Event;
import rs.ftn.uns.novisad.model.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [K6] Uslovi pretrage dogadjaja.
 * <p>
 * Filteri se grade dinamicki: uslov se dodaje samo kada je parametar zadat.
 * Time se izbegava obrazac "(:param is null or ...)", koji PostgreSQL odbija
 * jer ne moze da zakljuci tip null parametra.
 */
public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> search(String query,
                                              EventType type,
                                              Long locationId,
                                              LocalDateTime from,
                                              LocalDateTime to,
                                              Boolean freeEntry,
                                              BigDecimal minPrice,
                                              BigDecimal maxPrice) {
        return (root, criteriaQuery, cb) -> {
            // Mesto se dovlaci odmah, jer se dogadjaj mapira u DTO van transakcije.
            // Kod count upita fetch nije dozvoljen, pa se preskace.
            if (criteriaQuery != null && Long.class != criteriaQuery.getResultType()) {
                root.fetch("location", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("address")), pattern)));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("id"), locationId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), to));
            }
            if (freeEntry != null) {
                predicates.add(cb.equal(root.get("freeEntry"), freeEntry));
            }
            // Besplatni dogadjaji nemaju cenu, pa ispadaju iz filtriranja po ceni.
            if (minPrice != null) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("price")),
                        cb.greaterThanOrEqualTo(root.get("price"), minPrice)));
            }
            if (maxPrice != null) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("price")),
                        cb.lessThanOrEqualTo(root.get("price"), maxPrice)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
