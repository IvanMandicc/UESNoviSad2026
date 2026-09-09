package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.uns.novisad.dto.RegistrationRequestDto;
import rs.ftn.uns.novisad.exception.ApiException;
import rs.ftn.uns.novisad.model.AccountRequest;
import rs.ftn.uns.novisad.model.RequestStatus;
import rs.ftn.uns.novisad.model.Role;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.AccountRequestRepository;
import rs.ftn.uns.novisad.repository.UserRepository;

import java.time.Instant;
import java.util.List;

/**
 * [K1] Prijem zahteva za registraciju i [A1] njihova obrada od strane administratora.
 */
@Service
public class AccountRequestService {

    private static final Logger log = LoggerFactory.getLogger(AccountRequestService.class);

    private final AccountRequestRepository accountRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AccountRequestService(AccountRequestRepository accountRequestRepository,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 EmailService emailService) {
        this.accountRequestRepository = accountRequestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** [K1] Neregistrovan korisnik salje zahtev za registraciju. */
    @Transactional
    public AccountRequest submit(RegistrationRequestDto dto) {
        String email = dto.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Nalog sa ovom email adresom vec postoji.");
        }
        if (accountRequestRepository.existsByEmailIgnoreCaseAndStatus(email, RequestStatus.PENDING)) {
            throw ApiException.conflict("Zahtev sa ovom email adresom je vec poslat i ceka obradu.");
        }

        AccountRequest request = AccountRequest.builder()
                .email(email)
                .password(passwordEncoder.encode(dto.password()))
                .firstName(dto.firstName().trim())
                .lastName(dto.lastName().trim())
                .city(trimOrNull(dto.city()))
                .phone(trimOrNull(dto.phone()))
                .status(RequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        AccountRequest saved = accountRequestRepository.save(request);
        log.info("Primljen zahtev za registraciju [id={}, email={}]", saved.getId(), saved.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AccountRequest> findPending() {
        return accountRequestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<AccountRequest> findAll() {
        return accountRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    /** [A1] Administrator prihvata zahtev - kreira se nalog korisnika. */
    @Transactional
    public User approve(Long id) {
        AccountRequest request = loadPending(id);

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw ApiException.conflict("Nalog sa ovom email adresom je u medjuvremenu vec kreiran.");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setProcessedAt(Instant.now());
        accountRequestRepository.save(request);

        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .city(request.getCity())
                .phone(request.getPhone())
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Zahtev za registraciju prihvacen [requestId={}, userId={}, email={}]",
                request.getId(), saved.getId(), saved.getEmail());
        emailService.sendRegistrationApproved(saved);
        return saved;
    }

    /** [A1] Administrator odbija zahtev. */
    @Transactional
    public AccountRequest reject(Long id, String reason) {
        AccountRequest request = loadPending(id);

        request.setStatus(RequestStatus.REJECTED);
        request.setProcessedAt(Instant.now());
        request.setRejectionReason(trimOrNull(reason));

        AccountRequest saved = accountRequestRepository.save(request);
        log.info("Zahtev za registraciju odbijen [requestId={}, email={}]", saved.getId(), saved.getEmail());
        emailService.sendRegistrationRejected(saved);
        return saved;
    }

    private AccountRequest loadPending(Long id) {
        AccountRequest request = accountRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Zahtev za registraciju nije pronadjen."));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw ApiException.conflict("Zahtev je vec obradjen.");
        }
        return request;
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
