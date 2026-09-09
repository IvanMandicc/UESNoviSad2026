package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rs.ftn.uns.novisad.dto.EventDto;
import rs.ftn.uns.novisad.dto.EventFormDto;
import rs.ftn.uns.novisad.model.Event;
import rs.ftn.uns.novisad.model.EventType;
import rs.ftn.uns.novisad.service.EventService;
import rs.ftn.uns.novisad.storage.StorageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** [K4] / [M1] Rukovanje dogadjajima. */
@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;
    private final StorageService storageService;

    public EventController(EventService eventService, StorageService storageService) {
        this.eventService = eventService;
        this.storageService = storageService;
    }

    /**
     * Dogadjaji na mestu. Podrazumevano samo predstojeci [K3];
     * sa ?all=true i oni koji su se vec odrzali.
     */
    @GetMapping("/locations/{locationId}/events")
    public ResponseEntity<List<EventDto>> byLocation(@PathVariable Long locationId,
                                                     @RequestParam(name = "all", defaultValue = "false") boolean all) {
        List<Event> events = all
                ? eventService.findAllByLocation(locationId)
                : eventService.findUpcomingByLocation(locationId);
        return ResponseEntity.ok(events.stream().map(EventDto::from).toList());
    }

    /** [K4] Dodavanje dogadjaja - menadzer tog mesta (ili administrator). */
    @PostMapping(path = "/locations/{locationId}/events", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDto> create(@PathVariable Long locationId,
                                           @Valid @ModelAttribute EventFormDto form,
                                           Authentication authentication) {
        Event created = eventService.create(locationId, form, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(EventDto.from(created));
    }

    /**
     * [K6] Stranica za dogadjaje. Bez parametara vraca danasnje dogadjaje sa svih
     * mesta; moguce je pretraziti po nazivu i adresi i filtrirati po tipu, mestu,
     * ceni i proizvoljnom datumu u proslosti ili buducnosti.
     */
    @GetMapping("/events")
    public ResponseEntity<List<EventDto>> list(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "type", required = false) EventType type,
            @RequestParam(name = "locationId", required = false) Long locationId,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "freeEntry", required = false) Boolean freeEntry,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "allDates", defaultValue = "false") boolean allDates) {

        List<Event> events = eventService.search(
                query, type, locationId, date, freeEntry, minPrice, maxPrice, allDates);
        return ResponseEntity.ok(events.stream().map(EventDto::from).toList());
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventDto> details(@PathVariable Long id) {
        Event event = eventService.findById(id);
        return ResponseEntity.ok(EventDto.withTimesHeld(event, eventService.countTimesHeld(event)));
    }

    /** Slika dogadjaja. Javna, da bi <img> tag radio bez Authorization zaglavlja. */
    @GetMapping("/events/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id) {
        Event event = eventService.findById(id);
        Resource resource = storageService.load(event.getImageKey());
        String contentType = storageService.contentType(event.getImageKey());

        return ResponseEntity.ok()
                .contentType(contentType != null
                        ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** [K4] Izmena dogadjaja - menadzer tog mesta (ili administrator). */
    @PutMapping(path = "/events/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDto> update(@PathVariable Long id,
                                           @Valid @ModelAttribute EventFormDto form,
                                           Authentication authentication) {
        return ResponseEntity.ok(EventDto.from(eventService.update(id, form, authentication.getName())));
    }

    /** [K4] Uklanjanje dogadjaja - menadzer tog mesta (ili administrator). */
    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        eventService.deactivate(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /** Da li prijavljeni korisnik sme da rukuje dogadjajima na datom mestu (za prikaz dugmadi). */
    @GetMapping("/locations/{locationId}/events/permissions")
    public ResponseEntity<Boolean> canManage(@PathVariable Long locationId, Authentication authentication) {
        return ResponseEntity.ok(eventService.canManageEvents(locationId, authentication.getName()));
    }
}
