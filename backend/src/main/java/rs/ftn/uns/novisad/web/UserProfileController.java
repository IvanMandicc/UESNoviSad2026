package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ftn.uns.novisad.dto.*;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.service.ReviewService;
import rs.ftn.uns.novisad.service.UserProfileService;
import rs.ftn.uns.novisad.storage.StorageService;

import java.util.List;

/** [K9] Promena lozinke i [K10] profil korisnika. */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService profileService;
    private final ReviewService reviewService;
    private final StorageService storageService;

    public UserProfileController(UserProfileService profileService,
                                 ReviewService reviewService,
                                 StorageService storageService) {
        this.profileService = profileService;
        this.reviewService = reviewService;
        this.storageService = storageService;
    }

    /** [K10] Profil prijavljenog korisnika: podaci, utisci i mesta kojima upravlja. */
    @GetMapping("/me")
    public ResponseEntity<ProfileDto> profile(Authentication authentication) {
        User user = profileService.findByEmail(authentication.getName());

        List<ReviewDto> reviews = profileService.findReviews(user.getId()).stream()
                .map(review -> ReviewDto.from(review, reviewService.findCommentText(review.getId())))
                .toList();

        List<LocationDto> managed = profileService.findManagedLocations(user.getId()).stream()
                .map(location -> LocationDto.summary(
                        location,
                        reviewService.findAverageRating(location.getId()),
                        reviewService.countReviews(location.getId())))
                .toList();

        return ResponseEntity.ok(new ProfileDto(UserDto.from(user), reviews, managed));
    }

    /** [K10] Promena dodatnih podataka na profilu. */
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileDto dto,
                                                 Authentication authentication) {
        User updated = profileService.updateProfile(authentication.getName(), dto);
        return ResponseEntity.ok(UserDto.from(updated));
    }

    /** [K10] Promena slike profila. */
    @PostMapping(path = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateImage(@RequestPart("image") MultipartFile image,
                                               Authentication authentication) {
        User updated = profileService.updateImage(authentication.getName(), image);
        return ResponseEntity.ok(UserDto.from(updated));
    }

    /** Slika profila. Javna, da bi <img> tag radio bez Authorization zaglavlja. */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id) {
        User user = profileService.findById(id);
        if (user.getImageKey() == null) {
            throw ApiException.notFound("Korisnik nema postavljenu sliku.");
        }

        Resource resource = storageService.load(user.getImageKey());
        String contentType = storageService.contentType(user.getImageKey());

        return ResponseEntity.ok()
                .contentType(contentType != null
                        ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** [K9] Promena lozinke. */
    @PostMapping("/me/password")
    public ResponseEntity<MessageDto> changePassword(@Valid @RequestBody ChangePasswordDto dto,
                                                     Authentication authentication) {
        profileService.changePassword(authentication.getName(), dto);
        return ResponseEntity.ok(new MessageDto("Lozinka je uspesno promenjena."));
    }
}
