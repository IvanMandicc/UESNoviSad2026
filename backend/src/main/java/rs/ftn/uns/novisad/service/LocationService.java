package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.uns.novisad.dto.LocationAttributesDto;
import rs.ftn.uns.novisad.dto.LocationFormDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.model.Role;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.LocationRepository;
import rs.ftn.uns.novisad.repository.ManagesRepository;
import rs.ftn.uns.novisad.repository.UserRepository;
import rs.ftn.uns.novisad.storage.StorageService;

import java.time.Instant;
import java.util.List;

/**
 * [K3] Rukovanje mestima.
 * <p>
 * Mestom u potpunosti rukuje administrator sistema; menadzer mesta sme da azurira
 * samo atribute (adresa, tip mesta, opis) mesta kojim upravlja.
 */
@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);
    private static final String IMAGE_FOLDER = "locations";

    private final LocationRepository locationRepository;
    private final ManagesRepository managesRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public LocationService(LocationRepository locationRepository,
                           ManagesRepository managesRepository,
                           UserRepository userRepository,
                           StorageService storageService) {
        this.locationRepository = locationRepository;
        this.managesRepository = managesRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<Location> findAll() {
        return locationRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Location findById(Long id) {
        return locationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ApiException.notFound("Mesto nije pronadjeno."));
    }

    /** [K3] Dodavanje mesta - samo administrator. Slika je obavezna. */
    @Transactional
    public Location create(LocationFormDto form) {
        String name = form.getName().trim();
        if (locationRepository.existsByNameIgnoreCaseAndActiveTrue(name)) {
            throw ApiException.conflict("Mesto sa ovim nazivom vec postoji.");
        }
        if (form.getImage() == null || form.getImage().isEmpty()) {
            throw ApiException.badRequest("Slika mesta je obavezna.");
        }

        String imageKey = storageService.store(form.getImage(), IMAGE_FOLDER);

        Location location = Location.builder()
                .name(name)
                .address(form.getAddress().trim())
                .type(form.getType())
                .description(form.getDescription().trim())
                .imageKey(imageKey)
                .active(true)
                .createdAt(Instant.now())
                .build();

        Location saved = locationRepository.save(location);
        log.info("Kreirano mesto [id={}, naziv={}]", saved.getId(), saved.getName());
        return saved;
    }

    /** [K3] Izmena svih podataka mesta - samo administrator. */
    @Transactional
    public Location update(Long id, LocationFormDto form) {
        Location location = findById(id);
        String name = form.getName().trim();

        if (locationRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(name, id)) {
            throw ApiException.conflict("Drugo mesto sa ovim nazivom vec postoji.");
        }

        location.setName(name);
        location.setAddress(form.getAddress().trim());
        location.setType(form.getType());
        location.setDescription(form.getDescription().trim());

        // Slika se menja samo ako je nova poslata; stara se brise tek nakon uspesnog upisa.
        if (form.getImage() != null && !form.getImage().isEmpty()) {
            String previousKey = location.getImageKey();
            location.setImageKey(storageService.store(form.getImage(), IMAGE_FOLDER));
            storageService.delete(previousKey);
        }

        Location saved = locationRepository.save(location);
        log.info("Azurirano mesto [id={}, naziv={}]", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * [K3] Azuriranje atributa mesta - dozvoljeno administratoru i menadzeru tog mesta.
     * Naziv i slika se ovim putem ne menjaju.
     */
    @Transactional
    public Location updateAttributes(Long id, LocationAttributesDto dto, String actingUserEmail) {
        Location location = findById(id);
        User actor = loadUser(actingUserEmail);

        if (actor.getRole() != Role.ADMIN && !managesRepository.existsByUserIdAndLocationId(actor.getId(), id)) {
            throw ApiException.forbidden("Niste menadzer ovog mesta.");
        }

        location.setAddress(dto.address().trim());
        location.setType(dto.type());
        location.setDescription(dto.description().trim());

        Location saved = locationRepository.save(location);
        log.info("Menadzer/admin azurirao atribute mesta [id={}, korisnik={}]", id, actingUserEmail);
        return saved;
    }

    /**
     * [K3] Uklanjanje mesta - samo administrator. Brise se logicki, u skladu sa
     * napomenom iz specifikacije da se podaci cuvaju neograniceno.
     */
    @Transactional
    public void deactivate(Long id) {
        Location location = findById(id);
        location.setActive(false);
        locationRepository.save(location);
        log.info("Mesto logicki uklonjeno [id={}, naziv={}]", id, location.getName());
    }

    private User loadUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronadjen."));
    }
}
