package rs.ftn.uns.novisad.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.ftn.uns.novisad.repository.LocationRepository;

/**
 * [UES] Osvezava indeks kada se promene utisci mesta.
 * <p>
 * Slusa tek nakon uspesnog commit-a, da bi se prosecne ocene racunale nad
 * podacima koji su zaista upisani u bazu.
 */
@Component
public class LocationIndexListener {

    private static final Logger log = LoggerFactory.getLogger(LocationIndexListener.class);

    private final LocationIndexService indexService;
    private final LocationRepository locationRepository;

    public LocationIndexListener(LocationIndexService indexService, LocationRepository locationRepository) {
        this.indexService = indexService;
        this.locationRepository = locationRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewsChanged(ReviewsChangedEvent event) {
        locationRepository.findByIdAndActiveTrue(event.locationId()).ifPresentOrElse(
                indexService::index,
                () -> log.debug("[UES] Mesto {} vise nije aktivno, preskacem indeksiranje.", event.locationId()));
    }
}
