package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.NotNull;

/** [A2] Dodeljivanje korisnika kao menadzera mesta. */
public record AssignManagerDto(
        @NotNull(message = "Korisnik je obavezan.")
        Long userId
) {
}
