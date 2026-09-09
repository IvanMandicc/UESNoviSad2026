package rs.ftn.uns.novisad.dto;

import rs.ftn.uns.novisad.model.Rate;
import rs.ftn.uns.novisad.model.Review;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

/** [K5] Prikaz utiska. */
public record ReviewDto(
        Long id,
        Long locationId,
        String locationName,
        Long eventId,
        String eventName,
        LocalDateTime eventDate,
        Long authorId,
        String authorName,
        /** Ocene po stavkama; stavka koju korisnik nije ocenio se ne pojavljuje. */
        Map<String, Integer> rates,
        /** Prosek datih ocena na ovom utisku. */
        Double averageRate,
        String comment,
        /** [K5] Koliko se puta dogadjaj odrzao u trenutku pisanja utiska. */
        long timesHeldAtReview,
        boolean hidden,
        Instant createdAt
) {

    public static ReviewDto from(Review review, String comment) {
        Map<String, Integer> rates = new LinkedHashMap<>();
        for (Rate rate : review.getRates()) {
            rates.put(rate.getCategory().name(), rate.getValue());
        }

        OptionalDouble average = review.getRates().stream().mapToInt(Rate::getValue).average();

        return new ReviewDto(
                review.getId(),
                review.getLocation().getId(),
                review.getLocation().getName(),
                review.getEvent().getId(),
                review.getEvent().getName(),
                review.getEvent().getDate(),
                review.getAuthor().getId(),
                review.getAuthor().getFirstName() + " " + review.getAuthor().getLastName(),
                rates,
                average.isPresent() ? average.getAsDouble() : null,
                comment,
                review.getTimesHeldAtReview(),
                review.isHidden(),
                review.getCreatedAt()
        );
    }
}
