package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ftn.uns.novisad.dto.AccountRequestDto;
import rs.ftn.uns.novisad.dto.RejectRequestDto;
import rs.ftn.uns.novisad.dto.UserDto;
import rs.ftn.uns.novisad.model.AccountRequest;
import rs.ftn.uns.novisad.service.AccountRequestService;

import java.util.List;

/** [A1] Obrada pristiglih zahteva za registraciju - samo administrator. */
@RestController
@RequestMapping("/api/admin/registration-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountRequestController {

    private final AccountRequestService accountRequestService;

    public AdminAccountRequestController(AccountRequestService accountRequestService) {
        this.accountRequestService = accountRequestService;
    }

    /** Podrazumevano vraca zahteve na cekanju; ?all=true vraca sve. */
    @GetMapping
    public ResponseEntity<List<AccountRequestDto>> list(
            @RequestParam(name = "all", defaultValue = "false") boolean all) {
        List<AccountRequest> requests = all ? accountRequestService.findAll() : accountRequestService.findPending();
        return ResponseEntity.ok(requests.stream().map(AccountRequestDto::from).toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<UserDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(UserDto.from(accountRequestService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AccountRequestDto> reject(@PathVariable Long id,
                                                    @Valid @RequestBody(required = false) RejectRequestDto dto) {
        String reason = dto == null ? null : dto.reason();
        return ResponseEntity.ok(AccountRequestDto.from(accountRequestService.reject(id, reason)));
    }
}
