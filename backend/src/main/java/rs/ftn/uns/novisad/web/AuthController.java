package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rs.ftn.uns.novisad.dto.AuthResponseDto;
import rs.ftn.uns.novisad.dto.LoginRequestDto;
import rs.ftn.uns.novisad.dto.MessageDto;
import rs.ftn.uns.novisad.dto.UserDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.UserRepository;
import rs.ftn.uns.novisad.service.AuthService;

/** [K2] Prijava i odjava sa sistema. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    /**
     * [K2] Odjava. Token je stateless, pa se odjava svodi na brisanje tokena
     * na klijentu; ovde ciscimo SecurityContext i belezimo dogadjaj.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageDto> logout(Authentication authentication) {
        if (authentication != null) {
            log.info("Odjava korisnika [email={}]", authentication.getName());
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageDto("Uspesno ste se odjavili."));
    }

    /** Podaci o trenutno prijavljenom korisniku. */
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronadjen."));
        return ResponseEntity.ok(UserDto.from(user));
    }
}
