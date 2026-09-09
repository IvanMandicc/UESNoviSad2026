package rs.ftn.uns.novisad.dto;

import rs.ftn.uns.novisad.model.Location;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * [K3] Prikaz mesta, sa ukupnom srednjom vrednoscu ocene.
 * <p>
 * {@code averageRating} je null dok mesto nema nijednu ocenu. Prosek po stavkama
 * ({@code averageByCategory}) se popunjava samo na stranici mesta.
 */
public record LocationDto(
        Long id,
        String name,
        String address,
        String type,
        String description,
        String imageUrl,
        Double averageRating,
        Long reviewCount,
        Map<String, Double> averageByCategory,
        List<ManagerDto> managers,
        Instant createdAt,
        Instant updatedAt
) {

    /** Kratak prikaz za listu mesta - bez menadzera i bez proseka po stavkama. */
    public static LocationDto summary(Location location, Double averageRating, Long reviewCount) {
        return new LocationDto(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getType().name(),
                location.getDescription(),
                imageUrl(location),
                averageRating,
                reviewCount == null ? 0L : reviewCount,
                null,
                List.of(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }

    /** Detaljan prikaz za stranicu mesta. */
    public static LocationDto details(Location location,
                                      List<ManagerDto> managers,
                                      Double averageRating,
                                      long reviewCount,
                                      Map<String, Double> averageByCategory) {
        return new LocationDto(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getType().name(),
                location.getDescription(),
                imageUrl(location),
                averageRating,
                reviewCount,
                averageByCategory,
                managers,
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }

    private static String imageUrl(Location location) {
        return "/api/locations/" + location.getId() + "/image";
    }
}
