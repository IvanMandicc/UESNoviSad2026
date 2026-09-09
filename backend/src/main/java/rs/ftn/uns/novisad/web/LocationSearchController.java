package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ftn.uns.novisad.dto.LocationSearchDto;
import rs.ftn.uns.novisad.dto.LocationSearchResultDto;
import rs.ftn.uns.novisad.dto.MessageDto;
import rs.ftn.uns.novisad.search.LocationIndexService;
import rs.ftn.uns.novisad.search.LocationSearchService;

import java.util.List;

/** [S1] Pretraga mesta u Elasticsearch-u. */
@RestController
@RequestMapping("/api/search")
public class LocationSearchController {

    private final LocationSearchService searchService;
    private final LocationIndexService indexService;

    public LocationSearchController(LocationSearchService searchService,
                                    LocationIndexService indexService) {
        this.searchService = searchService;
        this.indexService = indexService;
    }

    /**
     * [S1] Pretraga mesta. Sva polja su opciona.
     * <p>
     * Tekstualna polja prihvataju: {@code "tacna fraza"}, {@code prefiks*}
     * i {@code ~fuzzy}. Operator AND/OR odredjuje kako se kombinuju.
     */
    @PostMapping("/locations")
    public ResponseEntity<List<LocationSearchResultDto>> search(@Valid @RequestBody LocationSearchDto criteria) {
        return ResponseEntity.ok(searchService.search(criteria));
    }

    /** [S1] Slicna mesta na osnovu naziva, opisa i sadrzaja PDF-a. */
    @GetMapping("/locations/{id}/similar")
    public ResponseEntity<List<LocationSearchResultDto>> similar(@PathVariable Long id) {
        return ResponseEntity.ok(searchService.moreLikeThis(id));
    }

    /**
     * Ponovno indeksiranje svih mesta - administratorska alatka, korisna posle
     * promene mapiranja ili kada se indeks razidje sa relacionom bazom.
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageDto> reindex() {
        int count = indexService.reindexAll();
        return ResponseEntity.ok(new MessageDto("Ponovo indeksirano mesta: " + count));
    }
}
