package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.model.Manages;
import rs.ftn.uns.novisad.model.Role;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.LocationRepository;
import rs.ftn.uns.novisad.repository.ManagesRepository;
import rs.ftn.uns.novisad.repository.UserRepository;

import java.time.Instant;
import java.util.List;

/**
 * [A2] Upravljanje menadzerima mesta - iskljucivo administrator sistema.
 * <p>
 * Uloga na korisniku prati stanje u tabeli {@code manages}: korisnik postaje
 * MANAGER kada dobije prvo mesto, a vraca se na USER kada mu se ukloni poslednje.
 * Administratoru se uloga nikada ne menja.
 */
@Service
public class ManagerService {

    private static final Logger log = LoggerFactory.getLogger(ManagerService.class);

    private final ManagesRepository managesRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    public ManagerService(ManagesRepository managesRepository,
                          LocationRepository locationRepository,
                          UserRepository userRepository) {
        this.managesRepository = managesRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Manages> findByLocation(Long locationId) {
        return managesRepository.findByLocationIdOrderByAssignedAtAsc(locationId);
    }

    @Transactional(readOnly = true)
    public List<Location> findLocationsManagedBy(Long userId) {
        return managesRepository.findActiveLocationsManagedBy(userId);
    }

    /** [A2] Dodeljivanje korisniku uloge menadzera na datom mestu. */
    @Transactional
    public Manages assign(Long locationId, Long userId) {
        Location location = locationRepository.findByIdAndActiveTrue(locationId)
                .orElseThrow(() -> ApiException.notFound("Mesto nije pronadjeno."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronadjen."));

        if (user.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Administrator sistema ne moze biti menadzer mesta.");
        }
        if (managesRepository.existsByUserIdAndLocationId(userId, locationId)) {
            throw ApiException.conflict("Korisnik je vec menadzer ovog mesta.");
        }

        Manages manages = managesRepository.save(Manages.builder()
                .user(user)
                .location(location)
                .assignedAt(Instant.now())
                .build());

        if (user.getRole() != Role.MANAGER) {
            user.setRole(Role.MANAGER);
            userRepository.save(user);
        }

        log.info("Korisnik postavljen za menadzera [userId={}, email={}, locationId={}]",
                userId, user.getEmail(), locationId);
        return manages;
    }

    /**
     * [A2] Uklanjanje menadzera sa mesta. Ako mu je to bilo poslednje mesto,
     * vraca se na ulogu obicnog korisnika.
     */
    @Transactional
    public void remove(Long locationId, Long userId) {
        Manages manages = managesRepository.findByUserIdAndLocationId(userId, locationId)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije menadzer ovog mesta."));

        User user = manages.getUser();
        managesRepository.delete(manages);
        managesRepository.flush();

        long remaining = managesRepository.countByUserId(userId);
        if (remaining == 0 && user.getRole() == Role.MANAGER) {
            user.setRole(Role.USER);
            userRepository.save(user);
            log.info("Menadzer uklonjen sa poslednjeg mesta i vracen na ulogu USER [userId={}]", userId);
        }

        log.info("Menadzer uklonjen sa mesta [userId={}, locationId={}, preostalo mesta={}]",
                userId, locationId, remaining);
    }
}
