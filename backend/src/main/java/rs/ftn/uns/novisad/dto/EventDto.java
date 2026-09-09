package rs.ftn.uns.novisad.dto;

import rs.ftn.uns.novisad.model.Event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/** [K4] Prikaz dogadjaja. */
public record EventDto(
        Long id,
        String name,
        Long locationId,
        String locationName,
        String address,
        String type,
        LocalDateTime date,
        boolean regular,
        boolean freeEntry,
        BigDecimal price,
        String imageUrl,
        boolean hasTakenPlace,
        /** [K5] Koliko se puta dogadjaj ukupno odrzao do sada. */
        Long timesHeld,
        Instant createdAt,
        Instant updatedAt
) {

    public static EventDto from(Event event) {
        return build(event, null);
    }

    public static EventDto withTimesHeld(Event event, long timesHeld) {
        return build(event, timesHeld);
    }

    private static EventDto build(Event event, Long timesHeld) {
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getLocation().getId(),
                event.getLocation().getName(),
                event.getAddress(),
                event.getType().name(),
                event.getDate(),
                event.isRegular(),
                event.isFreeEntry(),
                event.getPrice(),
                "/api/events/" + event.getId() + "/image",
                event.hasTakenPlace(),
                timesHeld,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
