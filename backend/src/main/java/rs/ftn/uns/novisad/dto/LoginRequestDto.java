package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** [K2] Telo zahteva za prijavu. */
public record LoginRequestDto(

        @NotBlank(message = "Email je obavezan.")
        @Email(message = "Email nije u ispravnom formatu.")
        String email,

        @NotBlank(message = "Lozinka je obavezna.")
        String password
) {
}
