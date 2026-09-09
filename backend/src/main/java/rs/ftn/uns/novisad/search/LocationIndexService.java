package rs.ftn.uns.novisad.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.repository.LocationRepository;
import rs.ftn.uns.novisad.service.ReviewService;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [UES] Odrzavanje Elasticsearch indeksa mesta.
 * <p>
 * Indeks se osvezava pri svakoj izmeni mesta i pri promeni ocena, da pretraga
 * uvek radi nad tekucim stanjem. Greska pri indeksiranju se belezi, ali ne obara
 * poslovnu operaciju - relaciona baza ostaje izvor istine.
 */
@Service
public class LocationIndexService {

    private static final Logger log = LoggerFactory.getLogger(LocationIndexService.class);

    private final ElasticsearchOperations operations;
    private final LocationRepository locationRepository;
    private final ReviewService reviewService;

    /** Iskljucuje se u testovima, da ne traze pokrenut Elasticsearch. */
    @Value("${app.search.enabled}")
    private boolean enabled;

    public LocationIndexService(ElasticsearchOperations operations,
                                LocationRepository locationRepository,
                                ReviewService reviewService) {
        this.operations = operations;
        this.locationRepository = locationRepository;
        this.reviewService = reviewService;
    }

    /**
     * Indeksira jedno mesto. Svi podaci se citaju iz relacione baze, ukljucujuci
     * tekst izvucen iz PDF-a, pa ponovno indeksiranje ne gubi nista.
     */
    @Transactional(readOnly = true)
    public void index(Location location) {
        if (!enabled) {
            return;
        }
        try {
            Map<String, Double> byCategory = reviewService.findAverageRatingByCategory(location.getId());

            LocationDocument document = LocationDocument.builder()
                    .id(String.valueOf(location.getId()))
                    .name(location.getName())
                    .nameSort(sortKey(location.getName()))
                    .description(location.getDescription())
                    .address(location.getAddress())
                    .type(location.getType().name())
                    .pdfContent(location.getPdfContent())
                    .pdfKey(location.getPdfKey())
                    .imageKey(location.getImageKey())
                    .reviewCount((int) reviewService.countReviews(location.getId()))
                    .ratingAverage(reviewService.findAverageRating(location.getId()))
                    .build();
            document.applyCategoryRatings(byCategory);

            operations.save(document);
            log.info("[UES] Indeksirano mesto [id={}, naziv={}, pdf={}]",
                    location.getId(), location.getName(), location.getPdfKey() != null);

        } catch (Exception ex) {
            log.error("[UES] Neuspesno indeksiranje mesta [id={}]: {}", location.getId(), ex.getMessage());
        }
    }

    /** Uklanja mesto iz indeksa - poziva se pri logickom brisanju. */
    public void remove(Long locationId) {
        if (!enabled) {
            return;
        }
        try {
            operations.delete(String.valueOf(locationId), LocationDocument.class);
            log.info("[UES] Mesto uklonjeno iz indeksa [id={}]", locationId);
        } catch (Exception ex) {
            log.warn("[UES] Neuspesno uklanjanje iz indeksa [id={}]: {}", locationId, ex.getMessage());
        }
    }

    /** Ponovno indeksiranje svih aktivnih mesta - korisno posle promene mapiranja. */
    @Transactional(readOnly = true)
    public int reindexAll() {
        if (!enabled) {
            log.warn("[UES] Indeksiranje je iskljuceno (app.search.enabled = false).");
            return 0;
        }
        IndexOperations indexOps = operations.indexOps(LocationDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();

        List<Location> locations = locationRepository.findByActiveTrueOrderByNameAsc();
        locations.forEach(this::index);

        log.info("[UES] Ponovo indeksirano {} mesta.", locations.size());
        return locations.size();
    }

    /**
     * Kljuc za sortiranje: mala slova, bez dijakritika i cirilice, da bi
     * abecedni redosled bio isti bez obzira na pismo kojim je naziv unet.
     */
    static String sortKey(String value) {
        if (value == null) {
            return null;
        }
        String latin = CyrillicTransliterator.toLatin(value);
        String stripped = Normalizer.normalize(latin, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped.toLowerCase(Locale.ROOT).trim();
    }
}
