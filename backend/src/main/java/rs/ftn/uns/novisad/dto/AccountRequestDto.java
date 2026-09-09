package rs.ftn.uns.novisad.dto;

import rs.ftn.uns.novisad.model.AccountRequest;

import java.time.Instant;

/** [A1] Prikaz zahteva za registraciju administratoru. */
public record AccountRequestDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String city,
        String phone,
        String status,
        Instant createdAt,
        Instant processedAt,
        String rejectionReason
) {
    public static AccountRequestDto from(AccountRequest r) {
        return new AccountRequestDto(
                r.getId(),
                r.getEmail(),
                r.getFirstName(),
                r.getLastName(),
                r.getCity(),
                r.getPhone(),
                r.getStatus().name(),
                r.getCreatedAt(),
                r.getProcessedAt(),
                r.getRejectionReason()
        );
    }
}
