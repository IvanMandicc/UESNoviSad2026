package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rs.ftn.uns.novisad.dto.CreateReviewDto;
import rs.ftn.uns.novisad.dto.EventDto;
import rs.ftn.uns.novisad.dto.ReviewDto;
import rs.ftn.uns.novisad.model.Review;
import rs.ftn.uns.novisad.service.EventService;
import rs.ftn.uns.novisad.service.ReviewService;

import java.util.List;

/** [K5] Ostavljanje i pregled utisaka. */
@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;
    private final EventService eventService;

    public ReviewController(ReviewService reviewService, EventService eventService) {
        this.reviewService = reviewService;
        this.eventService = eventService;
    }

    /** Utisci na mestu. Menadzer mesta i administrator vide i sakrivene [M2]. */
    @GetMapping("/locations/{locationId}/reviews")
    public ResponseEntity<List<ReviewDto>> byLocation(@PathVariable Long locationId,
                                                      Authentication authentication) {
        boolean includeHidden = eventService.canManageEvents(locationId, authentication.getName());
        List<ReviewDto> reviews = reviewService.findByLocation(locationId, includeHidden).stream()
                .map(review -> ReviewDto.from(review, reviewService.findCommentText(review.getId())))
                .toList();
        return ResponseEntity.ok(reviews);
    }

    /**
     * [K5] Dogadjaji na koje prijavljeni korisnik sme da ostavi utisak:
     * redovni, vec odrzani, i jos neocenjeni od strane tog korisnika.
     */
    @GetMapping("/locations/{locationId}/reviewable-events")
    public ResponseEntity<List<EventDto>> reviewableEvents(@PathVariable Long locationId,
                                                           Authentication authentication) {
        List<EventDto> events = reviewService.findReviewableEvents(locationId, authentication.getName())
                .stream()
                .map(event -> EventDto.withTimesHeld(event, eventService.countTimesHeld(event)))
                .toList();
        return ResponseEntity.ok(events);
    }

    /** [K5] Ostavljanje utiska na mesto. */
    @PostMapping("/locations/{locationId}/reviews")
    public ResponseEntity<ReviewDto> create(@PathVariable Long locationId,
                                            @Valid @RequestBody CreateReviewDto dto,
                                            Authentication authentication) {
        Review created = reviewService.create(locationId, dto, authentication.getName());
        ReviewDto body = ReviewDto.from(created, reviewService.findCommentText(created.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewDto> details(@PathVariable Long id) {
        Review review = reviewService.findById(id);
        return ResponseEntity.ok(ReviewDto.from(review, reviewService.findCommentText(id)));
    }
}
