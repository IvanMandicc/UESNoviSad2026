package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.uns.novisad.dto.EventFormDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.Event;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.model.Role;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.EventRepository;
import rs.ftn.uns.novisad.repository.LocationRepository;
import rs.ftn.uns.novisad.repository.ManagesRepository;
import rs.ftn.uns.novisad.repository.UserRepository;
import rs.ftn.uns.novisad.storage.StorageService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * [K4] / [M1] Rukovanje dogadjajima.
 * <p>
 * Po specifikaciji dogadjajima rukuje menadzer mesta. Administrator sistema ima
 * na raspolaganju sve funkcionalnosti menadzera, pa i on sme da rukuje dogadjajima.
 */
@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final String IMAGE_FOLDER = "events";

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final ManagesRepository managesRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public EventService(EventRepository eventRepository,
                        LocationRepository locationRepository,
                        ManagesRepository managesRepository,
                        UserRepository userRepository,
                        StorageService storageService) {
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.managesRepository = managesRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Event findById(Long id) {
        return eventRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ApiException.notFound("Dogadjaj nije pronadjen."));
    }

    /** [K3] Predstojeci dogadjaji na stranici mesta. */
    @Transactional(readOnly = true)
    public List<Event> findUpcomingByLocation(Long locationId) {
        return eventRepository.findByLocationIdAndActiveTrueAndDateAfterOrderByDateAsc(
                locationId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Event> findAllByLocation(Long locationId) {
        return eventRepository.findByLocationIdAndActiveTrueOrderByDateDesc(locationId);
    }

    /** [K8] Danasnji dogadjaji sa svih mesta. */
    @Transactional(readOnly = true)
    public List<Event> findToday() {
        LocalDate today = LocalDate.now();
        return eventRepository.findByActiveTrueAndDateBetweenOrderByDateAsc(
                today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    @Transactional(readOnly = true)
    public List<Event> findAll() {
        return eventRepository.findByActiveTrueOrderByDateAsc();
    }

    /**
     * [K5] Koliko se puta dogadjaj sa datim nazivom vec odrzao na svom mestu.
     * Redovan dogadjaj se vodi kao vise pojava sa istim nazivom i razlicitim datumima.
     */
    @Transactional(readOnly = true)
    public long countTimesHeld(Event event) {
        return eventRepository.countByNameIgnoreCaseAndLocationIdAndActiveTrueAndDateBefore(
                event.getName(), event.getLocation().getId(), LocalDateTime.now());
    }

    /** [K4] Dodavanje dogadjaja na mesto. Slika je obavezna. */
    @Transactional
    public Event create(Long locationId, EventFormDto form, String actingUserEmail) {
        Location location = locationRepository.findByIdAndActiveTrue(locationId)
                .orElseThrow(() -> ApiException.notFound("Mesto nije pronadjeno."));
        requireManagerOfLocation(locationId, actingUserEmail);
        validatePricing(form);

        if (form.getImage() == null || form.getImage().isEmpty()) {
            throw ApiException.badRequest("Slika dogadjaja je obavezna.");
        }

        Event event = Event.builder()
                .name(form.getName().trim())
                .location(location)
                .address(form.getAddress().trim())
                .type(form.getType())
                .date(form.getDate())
                .regular(Boolean.TRUE.equals(form.getRegular()))
                .freeEntry(Boolean.TRUE.equals(form.getFreeEntry()))
                .price(Boolean.TRUE.equals(form.getFreeEntry()) ? null : form.getPrice())
                .imageKey(storageService.store(form.getImage(), IMAGE_FOLDER))
                .active(true)
                .createdAt(Instant.now())
                .build();

        Event saved = eventRepository.save(event);
        log.info("Kreiran dogadjaj [id={}, naziv={}, mesto={}]",
                saved.getId(), saved.getName(), location.getName());
        return saved;
    }

    /** [K4] Izmena dogadjaja. */
    @Transactional
    public Event update(Long eventId, EventFormDto form, String actingUserEmail) {
        Event event = findById(eventId);
        requireManagerOfLocation(event.getLocation().getId(), actingUserEmail);
        validatePricing(form);

        event.setName(form.getName().trim());
        event.setAddress(form.getAddress().trim());
        event.setType(form.getType());
        event.setDate(form.getDate());
        event.setRegular(Boolean.TRUE.equals(form.getRegular()));
        event.setFreeEntry(Boolean.TRUE.equals(form.getFreeEntry()));
        event.setPrice(Boolean.TRUE.equals(form.getFreeEntry()) ? null : form.getPrice());

        // Stara slika se brise tek nakon uspesnog upisa nove.
        if (form.getImage() != null && !form.getImage().isEmpty()) {
            String previousKey = event.getImageKey();
            event.setImageKey(storageService.store(form.getImage(), IMAGE_FOLDER));
            storageService.delete(previousKey);
        }

        Event saved = eventRepository.save(event);
        log.info("Azuriran dogadjaj [id={}, naziv={}]", saved.getId(), saved.getName());
        return saved;
    }

    /** [K4] Uklanjanje dogadjaja (logicko brisanje). */
    @Transactional
    public void deactivate(Long eventId, String actingUserEmail) {
        Event event = findById(eventId);
        requireManagerOfLocation(event.getLocation().getId(), actingUserEmail);

        event.setActive(false);
        eventRepository.save(event);
        log.info("Dogadjaj logicki uklonjen [id={}, naziv={}]", eventId, event.getName());
    }

    /** Da li dati korisnik sme da rukuje dogadjajima na datom mestu. */
    @Transactional(readOnly = true)
    public boolean canManageEvents(Long locationId, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail).orElse(null);
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN
                || managesRepository.existsByUserIdAndLocationId(user.getId(), locationId);
    }

    private void requireManagerOfLocation(Long locationId, String userEmail) {
        if (!canManageEvents(locationId, userEmail)) {
            throw ApiException.forbidden("Dogadjajima na ovom mestu moze da rukuje samo njegov menadzer.");
        }
    }

    /** Cena je obavezna kada ulaz nije besplatan, i ne sme postojati kada jeste. */
    private void validatePricing(EventFormDto form) {
        boolean free = Boolean.TRUE.equals(form.getFreeEntry());
        if (!free && form.getPrice() == null) {
            throw ApiException.badRequest("Unesite cenu ulaska ili oznacite da je dogadjaj besplatan.");
        }
    }
}
