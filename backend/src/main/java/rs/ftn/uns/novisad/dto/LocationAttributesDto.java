package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import rs.ftn.uns.novisad.model.LocationType;

/**
 * [K3] Atributi koje menadzer mesta sme da azurira:
 * adresa, tip mesta i opis (ne i naziv).
 */
public record LocationAttributesDto(

        @NotBlank(message = "Adresa je obavezna.")
        @Size(max = 300)
        String address,

        @NotNull(message = "Tip mesta je obavezan.")
        LocationType type,

        @NotBlank(message = "Opis je obavezan.")
        @Size(max = 2000)
        String description
) {
}
