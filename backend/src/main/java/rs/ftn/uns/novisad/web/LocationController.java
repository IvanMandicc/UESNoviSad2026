package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rs.ftn.uns.novisad.dto.LocationAttributesDto;
import rs.ftn.uns.novisad.dto.LocationDto;
import rs.ftn.uns.novisad.dto.LocationFormDto;
import rs.ftn.uns.novisad.dto.ManagerDto;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.service.LocationService;
import rs.ftn.uns.novisad.service.ManagerService;
import rs.ftn.uns.novisad.storage.StorageService;

import java.util.List;

/** [K3] Rukovanje mestima. */
@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;
    private final ManagerService managerService;
    private final StorageService storageService;

    public LocationController(LocationService locationService,
                              ManagerService managerService,
                              StorageService storageService) {
        this.locationService = locationService;
        this.managerService = managerService;
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<List<LocationDto>> list() {
        return ResponseEntity.ok(locationService.findAll().stream().map(LocationDto::summary).toList());
    }

    /** Stranica mesta. Predstojeci dogadjaji [K4] i prosecna ocena [K5] dolaze kasnije. */
    @GetMapping("/{id}")
    public ResponseEntity<LocationDto> details(@PathVariable Long id) {
        Location location = locationService.findById(id);
        List<ManagerDto> managers = managerService.findByLocation(id).stream().map(ManagerDto::from).toList();
        return ResponseEntity.ok(LocationDto.details(location, managers));
    }

    /** Slika mesta. Javno dostupna da bi <img> tag radio bez Authorization zaglavlja. */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id) {
        Location location = locationService.findById(id);
        Resource resource = storageService.load(location.getImageKey());
        String contentType = storageService.contentType(location.getImageKey());

        return ResponseEntity.ok()
                .contentType(contentType != null
                        ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** [K3] Dodavanje mesta - jedino administrator sistema. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationDto> create(@Valid @ModelAttribute LocationFormDto form) {
        Location created = locationService.create(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(LocationDto.summary(created));
    }

    /** [K3] Izmena svih podataka mesta - jedino administrator sistema. */
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationDto> update(@PathVariable Long id,
                                              @Valid @ModelAttribute LocationFormDto form) {
        return ResponseEntity.ok(LocationDto.summary(locationService.update(id, form)));
    }

    /**
     * [K3] Azuriranje atributa (adresa, tip, opis).
     * Dozvoljeno administratoru i menadzeru tog mesta - provera je u servisu,
     * jer zavisi od toga kojim mestom korisnik upravlja.
     */
    @PatchMapping("/{id}/attributes")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<LocationDto> updateAttributes(@PathVariable Long id,
                                                        @Valid @RequestBody LocationAttributesDto dto,
                                                        Authentication authentication) {
        Location updated = locationService.updateAttributes(id, dto, authentication.getName());
        return ResponseEntity.ok(LocationDto.summary(updated));
    }

    /** [K3] Uklanjanje mesta - jedino administrator sistema (logicko brisanje). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
