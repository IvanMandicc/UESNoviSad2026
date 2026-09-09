package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** [K1] Telo zahteva za registraciju. */
public record RegistrationRequestDto(

        @NotBlank(message = "Email je obavezan.")
        @Email(message = "Email nije u ispravnom formatu.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Lozinka je obavezna.")
        @Size(min = 8, max = 64, message = "Lozinka mora imati izmedju 8 i 64 karaktera.")
        String password,

        @NotBlank(message = "Ime je obavezno.")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Prezime je obavezno.")
        @Size(max = 100)
        String lastName,

        @Size(max = 255)
        String city,

        @Size(max = 30)
        String phone
) {
}
