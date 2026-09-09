package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [K9] Promena lozinke: prvo trenutna lozinka, pa dva puta nova.
 * Poklapanje nove i potvrde se proverava u servisu.
 */
public record ChangePasswordDto(

        @NotBlank(message = "Trenutna lozinka je obavezna.")
        String currentPassword,

        @NotBlank(message = "Nova lozinka je obavezna.")
        @Size(min = 8, max = 64, message = "Nova lozinka mora imati izmedju 8 i 64 karaktera.")
        String newPassword,

        @NotBlank(message = "Potvrda nove lozinke je obavezna.")
        String confirmPassword
) {
}
