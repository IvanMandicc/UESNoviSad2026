package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ftn.uns.novisad.dto.LocationSearchDto;
import rs.ftn.uns.novisad.dto.LocationSearchResultDto;
import rs.ftn.uns.novisad.dto.MessageDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.search.LocationIndexService;
import rs.ftn.uns.novisad.search.LocationSearchService;

import java.util.List;

/** [S1] Pretraga mesta u Elasticsearch-u. */
@RestController
@RequestMapping("/api/search")
public class LocationSearchController {

    private final LocationSearchService searchService;
    private final LocationIndexService indexService;

    /**
     * Isto podesavanje koje iskljucuje indeksiranje (vidi LocationIndexService).
     * Kada je Elasticsearch nedostupan na masini (npr. UES deo nije instaliran),
     * ruta treba da vrati jasnu poruku umesto da propadne sa 500.
     */
    @Value("${app.search.enabled}")
    private boolean searchEnabled;

    public LocationSearchController(LocationSearchService searchService,
                                    LocationIndexService indexService) {
        this.searchService = searchService;
        this.indexService = indexService;
    }

    /**
     * [S1] Pretraga mesta po nazivu, opisu, sadrzaju PDF-a i opsegu broja utisaka.
     * Sva polja su opciona. Tekstualna polja se kombinuju BooleanQuery-jem sa
     * AND ili OR operatorom (parametar {@code operator}, podrazumevano AND).
     */
    @PostMapping("/locations")
    public ResponseEntity<List<LocationSearchResultDto>> search(@Valid @RequestBody LocationSearchDto criteria) {
        requireSearchEnabled();
        return ResponseEntity.ok(searchService.search(criteria));
    }

    // --- van trazenog obima: "more like this" ---
    // LocationSearchService.moreLikeThis() i dalje postoji i radi; ruta je
    // zakomentarisana jer ova funkcionalnost nije trazena.
    //
    // @GetMapping("/locations/{id}/similar")
    // public ResponseEntity<List<LocationSearchResultDto>> similar(@PathVariable Long id) {
    //     return ResponseEntity.ok(searchService.moreLikeThis(id));
    // }

    /**
     * Ponovno indeksiranje svih mesta - administratorska alatka, korisna posle
     * promene mapiranja ili kada se indeks razidje sa relacionom bazom.
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageDto> reindex() {
        requireSearchEnabled();
        int count = indexService.reindexAll();
        return ResponseEntity.ok(new MessageDto("Ponovo indeksirano mesta: " + count));
    }

    /**
     * Vraca 503 sa jasnom porukom umesto da propadne sa 500 kada Elasticsearch
     * nije dostupan na ovoj masini (app.search.enabled = false).
     */
    private void requireSearchEnabled() {
        if (!searchEnabled) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Pretraga mesta nije dostupna - Elasticsearch nije pokrenut na ovoj masini.");
        }
    }
}
