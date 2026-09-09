package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.uns.novisad.dto.LocationAttributesDto;
import rs.ftn.uns.novisad.dto.LocationFormDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.model.LocationType;
import rs.ftn.uns.novisad.model.Role;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.LocationRepository;
import rs.ftn.uns.novisad.repository.LocationSpecifications;
import rs.ftn.uns.novisad.repository.ManagesRepository;
import rs.ftn.uns.novisad.repository.UserRepository;
import rs.ftn.uns.novisad.search.LocationIndexService;
import rs.ftn.uns.novisad.search.PdfTextExtractor;
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
    private static final String PDF_FOLDER = "locations/pdf";

    private final LocationRepository locationRepository;
    private final ManagesRepository managesRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final LocationIndexService indexService;
    private final PdfTextExtractor pdfTextExtractor;

    public LocationService(LocationRepository locationRepository,
                           ManagesRepository managesRepository,
                           UserRepository userRepository,
                           StorageService storageService,
                           LocationIndexService indexService,
                           PdfTextExtractor pdfTextExtractor) {
        this.locationRepository = locationRepository;
        this.managesRepository = managesRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.indexService = indexService;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    @Transactional(readOnly = true)
    public List<Location> findAll() {
        return locationRepository.findByActiveTrueOrderByNameAsc();
    }

    /** [K6] Pretraga mesta po nazivu, adresi ili tipu mesta. */
    @Transactional(readOnly = true)
    public List<Location> search(String query, LocationType type) {
        return locationRepository.findAll(
                LocationSpecifications.search(query, type),
                Sort.by(Sort.Direction.ASC, "name"));
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

        // [UES] PDF je opcion; sadrzaj se parsira i indeksira kao Text polje.
        if (form.getPdf() != null && !form.getPdf().isEmpty()) {
            location.setPdfContent(pdfTextExtractor.extract(form.getPdf()));
            location.setPdfKey(storageService.store(form.getPdf(), PDF_FOLDER));
            location.setPdfFilename(form.getPdf().getOriginalFilename());
        }

        Location saved = locationRepository.save(location);
        indexService.index(saved);
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

        // [UES] Nov PDF zamenjuje stari; bez njega ostaje postojeci dokument.
        if (form.getPdf() != null && !form.getPdf().isEmpty()) {
            location.setPdfContent(pdfTextExtractor.extract(form.getPdf()));
            String previousPdfKey = location.getPdfKey();
            location.setPdfKey(storageService.store(form.getPdf(), PDF_FOLDER));
            location.setPdfFilename(form.getPdf().getOriginalFilename());
            storageService.delete(previousPdfKey);
        }

        Location saved = locationRepository.save(location);
        indexService.index(saved);
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
        indexService.index(saved);
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
        indexService.remove(id);
        log.info("Mesto logicki uklonjeno [id={}, naziv={}]", id, location.getName());
    }

    private User loadUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronadjen."));
    }

    /** Prazan pojam pretrage znaci "ne filtriraj po tome". */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
