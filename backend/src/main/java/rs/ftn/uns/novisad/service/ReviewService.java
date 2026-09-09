package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rs.ftn.uns.novisad.dto.CreateReviewDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.*;
import rs.ftn.uns.novisad.repository.*;
import rs.ftn.uns.novisad.search.ReviewsChangedEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [K5] Ostavljanje utiska na mesto.
 * <p>
 * Utisak se moze ostaviti samo na dogadjaj koji je oznacen kao redovan i koji se
 * vec odrzao na tom mestu u trenutku pisanja utiska.
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;
    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    public ReviewService(ReviewRepository reviewRepository,
                         CommentRepository commentRepository,
                         LocationRepository locationRepository,
                         EventRepository eventRepository,
                         UserRepository userRepository,
                         ApplicationEventPublisher events) {
        this.reviewRepository = reviewRepository;
        this.commentRepository = commentRepository;
        this.locationRepository = locationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.events = events;
    }

    /** Utisci na mestu. Sakriveni se prikazuju samo menadzeru mesta i administratoru [M2]. */
    @Transactional(readOnly = true)
    public List<Review> findByLocation(Long locationId, boolean includeHidden) {
        return includeHidden
                ? reviewRepository.findByLocationIdAndActiveTrueOrderByCreatedAtDesc(locationId)
                : reviewRepository.findByLocationIdAndActiveTrueAndHiddenFalseOrderByCreatedAtDesc(locationId);
    }

    @Transactional(readOnly = true)
    public Review findById(Long id) {
        return reviewRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ApiException.notFound("Utisak nije pronadjen."));
    }

    /** Komentar utiska; null ako korisnik nije ostavio komentar. */
    @Transactional(readOnly = true)
    public String findCommentText(Long reviewId) {
        return commentRepository
                .findFirstByReviewIdAndParentIsNullAndActiveTrueOrderByCreatedAtAsc(reviewId)
                .map(Comment::getText)
                .orElse(null);
    }

    /**
     * [K5] Dogadjaji na koje korisnik sme da ostavi utisak na datom mestu:
     * redovni dogadjaji koji su se vec odrzali, a na koje jos nije ostavio utisak.
     */
    @Transactional(readOnly = true)
    public List<Event> findReviewableEvents(Long locationId, String userEmail) {
        User user = loadUser(userEmail);
        return eventRepository.findByLocationIdAndActiveTrueOrderByDateDesc(locationId).stream()
                .filter(Event::isRegular)
                .filter(Event::hasTakenPlace)
                .filter(event -> !reviewRepository.existsByAuthorIdAndEventIdAndActiveTrue(
                        user.getId(), event.getId()))
                .toList();
    }

    /** [K5] Ostavljanje utiska. */
    @Transactional
    public Review create(Long locationId, CreateReviewDto dto, String userEmail) {
        Location location = locationRepository.findByIdAndActiveTrue(locationId)
                .orElseThrow(() -> ApiException.notFound("Mesto nije pronadjeno."));
        User author = loadUser(userEmail);

        Event event = eventRepository.findByIdAndActiveTrue(dto.eventId())
                .orElseThrow(() -> ApiException.notFound("Dogadjaj nije pronadjen."));

        if (!event.getLocation().getId().equals(locationId)) {
            throw ApiException.badRequest("Izabrani dogadjaj se ne odrzava na ovom mestu.");
        }
        if (!event.isRegular()) {
            throw ApiException.badRequest("Utisak se moze ostaviti samo na redovan dogadjaj.");
        }
        if (!event.hasTakenPlace()) {
            throw ApiException.badRequest("Utisak se moze ostaviti tek nakon sto se dogadjaj odrzi.");
        }
        if (reviewRepository.existsByAuthorIdAndEventIdAndActiveTrue(author.getId(), event.getId())) {
            throw ApiException.conflict("Vec ste ostavili utisak na ovaj dogadjaj.");
        }

        Map<RateCategory, Integer> values = collectRates(dto);
        if (values.isEmpty()) {
            throw ApiException.badRequest("Ocenite bar jednu stavku.");
        }

        long timesHeld = eventRepository.countByNameIgnoreCaseAndLocationIdAndActiveTrueAndDateBefore(
                event.getName(), locationId, LocalDateTime.now());

        Review review = Review.builder()
                .location(location)
                .event(event)
                .author(author)
                .timesHeldAtReview(timesHeld)
                .hidden(false)
                .active(true)
                .createdAt(Instant.now())
                .rates(new ArrayList<>())
                .build();

        values.forEach((category, value) -> review.getRates().add(Rate.builder()
                .review(review)
                .category(category)
                .value(value)
                .build()));

        Review saved = reviewRepository.save(review);

        if (StringUtils.hasText(dto.comment())) {
            commentRepository.save(Comment.builder()
                    .review(saved)
                    .author(author)
                    .text(dto.comment().trim())
                    .parent(null)
                    .active(true)
                    .createdAt(Instant.now())
                    .build());
        }

        log.info("Ostavljen utisak [id={}, mesto={}, dogadjaj={}, korisnik={}, ocena stavki={}]",
                saved.getId(), location.getName(), event.getName(), userEmail, values.size());

        // [UES] Broj utisaka i prosecne ocene u indeksu vise ne odgovaraju bazi.
        events.publishEvent(new ReviewsChangedEvent(location.getId()));
        return saved;
    }

    /** [K3] Ukupna, srednja vrednost ocene mesta; null kada mesto jos nema ocena. */
    @Transactional(readOnly = true)
    public Double findAverageRating(Long locationId) {
        return reviewRepository.findAverageRating(locationId);
    }

    @Transactional(readOnly = true)
    public long countReviews(Long locationId) {
        return reviewRepository.countByLocationIdAndActiveTrue(locationId);
    }

    /** Prosecne ocene za sva mesta odjednom - da lista mesta ne pravi upit po mestu. */
    @Transactional(readOnly = true)
    public Map<Long, Double> findAverageRatingForAllLocations() {
        Map<Long, Double> result = new LinkedHashMap<>();
        for (Object[] row : reviewRepository.findAverageRatingForAllLocations()) {
            result.put((Long) row[0], (Double) row[1]);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countReviewsForAllLocations() {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Object[] row : reviewRepository.countReviewsForAllLocations()) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    /** Prosek po stavkama - koristi se na stranici mesta i kasnije u UES pretrazi. */
    @Transactional(readOnly = true)
    public Map<String, Double> findAverageRatingByCategory(Long locationId) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : reviewRepository.findAverageRatingByCategory(locationId)) {
            result.put(((RateCategory) row[0]).name(), (Double) row[1]);
        }
        return result;
    }

    /** Uzima samo stavke koje je korisnik zaista ocenio. */
    private Map<RateCategory, Integer> collectRates(CreateReviewDto dto) {
        Map<RateCategory, Integer> values = new EnumMap<>(RateCategory.class);
        putIfPresent(values, RateCategory.PERFORMANCE, dto.performance());
        putIfPresent(values, RateCategory.SOUND_AND_LIGHT, dto.soundAndLight());
        putIfPresent(values, RateCategory.SPACE, dto.space());
        putIfPresent(values, RateCategory.OVERALL, dto.overall());
        return values;
    }

    private static void putIfPresent(Map<RateCategory, Integer> values, RateCategory category, Integer value) {
        if (value != null) {
            values.put(category, value);
        }
    }

    private User loadUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronadjen."));
    }
}
