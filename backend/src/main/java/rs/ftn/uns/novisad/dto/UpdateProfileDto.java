package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** [K10] Promena dodatnih podataka na profilu. Email se ne menja. */
public record UpdateProfileDto(

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
