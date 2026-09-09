package rs.ftn.uns.novisad.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.model.LocationType;

import java.util.ArrayList;
import java.util.List;

/**
 * [K6] Uslovi pretrage mesta po nazivu, adresi ili tipu mesta.
 * Uslov se dodaje samo kada je parametar zadat - vidi {@link EventSpecifications}.
 */
public final class LocationSpecifications {

    private LocationSpecifications() {
    }

    public static Specification<Location> search(String query, LocationType type) {
        return (root, criteriaQuery, cb) -> {
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
