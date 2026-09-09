package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.uns.novisad.dto.AuthResponseDto;
import rs.ftn.uns.novisad.dto.LoginRequestDto;
import rs.ftn.uns.novisad.dto.UserDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.AccountRequest;
import rs.ftn.uns.novisad.model.RequestStatus;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.AccountRequestRepository;
import rs.ftn.uns.novisad.repository.UserRepository;
import rs.ftn.uns.novisad.security.JwtService;

import java.util.Optional;

/** [K2] Prijava na sistem i izdavanje JWT tokena. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AccountRequestRepository accountRequestRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       AccountRequestRepository accountRequestRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.accountRequestRepository = accountRequestRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto dto) {
        String email = dto.email().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, dto.password()));
        } catch (AuthenticationException ex) {
            // Nalog jos ne postoji jer zahtev za registraciju nije obradjen (K1 / A1).
            explainPendingOrRejectedRequest(email);
            log.warn("Neuspesna prijava za email={}", email);
            throw new BadCredentialsException("Pogresan email ili lozinka.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Pogresan email ili lozinka."));

        String token = jwtService.generateToken(user);
        log.info("Uspesna prijava [userId={}, email={}, role={}]", user.getId(), user.getEmail(), user.getRole());

        return AuthResponseDto.of(token, jwtService.getExpirationSeconds(), UserDto.from(user));
    }

    /**
     * Ako korisnik nema nalog, ali ima zahtev u obradi ili odbijen zahtev,
     * vracamo jasniju poruku umesto generickog "pogresan email ili lozinka".
     */
    private void explainPendingOrRejectedRequest(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Optional<AccountRequest> latest =
                accountRequestRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(email);
        if (latest.isEmpty()) {
            return;
        }
        AccountRequest request = latest.get();
        if (request.getStatus() == RequestStatus.PENDING) {
            throw ApiException.forbidden("Vas zahtev za registraciju jos uvek nije obradjen.");
        }
        if (request.getStatus() == RequestStatus.REJECTED) {
            String reason = request.getRejectionReason();
            throw ApiException.forbidden(reason == null || reason.isBlank()
                    ? "Vas zahtev za registraciju je odbijen."
                    : "Vas zahtev za registraciju je odbijen. Razlog: " + reason);
        }
    }
}
