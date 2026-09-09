package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ftn.uns.novisad.dto.ChangePasswordDto;
import rs.ftn.uns.novisad.dto.UpdateProfileDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.Location;
import rs.ftn.uns.novisad.model.Review;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.ManagesRepository;
import rs.ftn.uns.novisad.repository.ReviewRepository;
import rs.ftn.uns.novisad.repository.UserRepository;
import rs.ftn.uns.novisad.storage.StorageService;

import java.util.List;

/**
 * [K9] Promena lozinke i [K10] promena podataka i slike na profilu.
 */
@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private static final String IMAGE_FOLDER = "users";

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ManagesRepository managesRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final EmailService emailService;

    public UserProfileService(UserRepository userRepository,
                              ReviewRepository reviewRepository,
                              ManagesRepository managesRepository,
                              PasswordEncoder passwordEncoder,
                              StorageService storageService,
                              EmailService emailService) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.managesRepository = managesRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageService = storageService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronadjen."));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronadjen."));
    }

    /** [K10] Spisak svih utisaka koje je korisnik ostavio. */
    @Transactional(readOnly = true)
    public List<Review> findReviews(Long userId) {
        return reviewRepository.findByAuthorIdAndActiveTrueOrderByCreatedAtDesc(userId);
    }

    /** [K10] Mesta na kojima je korisnik menadzer. */
    @Transactional(readOnly = true)
    public List<Location> findManagedLocations(Long userId) {
        return managesRepository.findActiveLocationsManagedBy(userId);
    }

    /** [K10] Promena dodatnih podataka na profilu. Email i uloga se ovim putem ne menjaju. */
    @Transactional
    public User updateProfile(String email, UpdateProfileDto dto) {
        User user = findByEmail(email);

        user.setFirstName(dto.firstName().trim());
        user.setLastName(dto.lastName().trim());
        user.setCity(trimOrNull(dto.city()));
        user.setPhone(trimOrNull(dto.phone()));

        User saved = userRepository.save(user);
        log.info("Azurirani podaci profila [userId={}, email={}]", saved.getId(), saved.getEmail());
        return saved;
    }

    /** [K10] Promena slike profila. */
    @Transactional
    public User updateImage(String email, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw ApiException.badRequest("Slika je obavezna.");
        }

        User user = findByEmail(email);
        String previousKey = user.getImageKey();

        // Stara slika se brise tek nakon uspesnog upisa nove.
        user.setImageKey(storageService.store(image, IMAGE_FOLDER));
        User saved = userRepository.save(user);
        storageService.delete(previousKey);

        log.info("Promenjena slika profila [userId={}]", saved.getId());
        return saved;
    }

    /**
     * [K9] Promena lozinke: proverava se trenutna lozinka, pa poklapanje
     * nove i njene potvrde. Nakon promene korisniku se salje mejl.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordDto dto) {
        User user = findByEmail(email);

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            log.warn("Neuspesna promena lozinke - pogresna trenutna lozinka [email={}]", email);
            throw ApiException.badRequest("Trenutna lozinka nije ispravna.");
        }
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw ApiException.badRequest("Nova lozinka i potvrda se ne poklapaju.");
        }
        if (passwordEncoder.matches(dto.newPassword(), user.getPassword())) {
            throw ApiException.badRequest("Nova lozinka mora biti razlicita od trenutne.");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        log.info("Promenjena lozinka [userId={}, email={}]", user.getId(), user.getEmail());
        emailService.sendPasswordChanged(user);
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
