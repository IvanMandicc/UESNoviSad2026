package rs.ftn.uns.novisad.dto;

import rs.ftn.uns.novisad.model.Location;

import java.time.Instant;
import java.util.List;

/**
 * [K3] Prikaz mesta.
 * <p>
 * Polja {@code averageRating} i {@code upcomingEvents} pripadaju zahtevima K5 i K4
 * koji jos nisu implementirani, pa su za sada prazni. Drze se u odgovoru da se
 * ugovor prema frontend-u ne menja kada ti zahtevi budu gotovi.
 */
public record LocationDto(
        Long id,
        String name,
        String address,
        String type,
        String description,
        String imageUrl,
        Double averageRating,
        Integer reviewCount,
        List<ManagerDto> managers,
        Instant createdAt,
        Instant updatedAt
) {

    /** Kratak prikaz za listu mesta - bez menadzera. */
    public static LocationDto summary(Location location) {
        return new LocationDto(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getType().name(),
                location.getDescription(),
                imageUrl(location),
                null,
                0,
                List.of(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }

    /** Detaljan prikaz za stranicu mesta - sa menadzerima. */
    public static LocationDto details(Location location, List<ManagerDto> managers) {
        return new LocationDto(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getType().name(),
                location.getDescription(),
                imageUrl(location),
                null,
                0,
                managers,
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }

    private static String imageUrl(Location location) {
        return "/api/locations/" + location.getId() + "/image";
    }
}
