package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ftn.uns.novisad.dto.AccountRequestDto;
import rs.ftn.uns.novisad.dto.RegistrationRequestDto;
import rs.ftn.uns.novisad.service.AccountRequestService;

/** [K1] Zahtev za registraciju korisnika - javno dostupno. */
@RestController
@RequestMapping("/api/registration-requests")
public class RegistrationRequestController {

    private final AccountRequestService accountRequestService;

    public RegistrationRequestController(AccountRequestService accountRequestService) {
        this.accountRequestService = accountRequestService;
    }

    @PostMapping
    public ResponseEntity<AccountRequestDto> submit(@Valid @RequestBody RegistrationRequestDto dto) {
        AccountRequestDto created = AccountRequestDto.from(accountRequestService.submit(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
