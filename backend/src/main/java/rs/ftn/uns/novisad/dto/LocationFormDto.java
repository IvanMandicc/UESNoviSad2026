package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import rs.ftn.uns.novisad.model.LocationType;

/**
 * [K3] Podaci forme za mesto (multipart/form-data zbog slike).
 * Klasa, a ne record, jer Spring vezuje polja forme preko setter-a.
 */
@Getter
@Setter
public class LocationFormDto {

    @NotBlank(message = "Naziv mesta je obavezan.")
    @Size(max = 200, message = "Naziv moze imati najvise 200 karaktera.")
    private String name;

    @NotBlank(message = "Adresa je obavezna.")
    @Size(max = 300, message = "Adresa moze imati najvise 300 karaktera.")
    private String address;

    @NotNull(message = "Tip mesta je obavezan.")
    private LocationType type;

    @NotBlank(message = "Opis je obavezan.")
    @Size(max = 2000, message = "Opis moze imati najvise 2000 karaktera.")
    private String description;

    /** Obavezna pri kreiranju; pri izmeni je opciona (bez nje slika ostaje ista). */
    private MultipartFile image;
}
