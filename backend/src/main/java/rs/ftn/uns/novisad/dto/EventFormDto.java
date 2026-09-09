package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import rs.ftn.uns.novisad.model.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [K4] Podaci forme za dogadjaj (multipart/form-data zbog slike).
 * Klasa, a ne record, jer Spring vezuje polja forme preko setter-a.
 */
@Getter
@Setter
public class EventFormDto {

    @NotBlank(message = "Naziv dogadjaja je obavezan.")
    @Size(max = 200, message = "Naziv moze imati najvise 200 karaktera.")
    private String name;

    @NotBlank(message = "Adresa je obavezna.")
    @Size(max = 300, message = "Adresa moze imati najvise 300 karaktera.")
    private String address;

    @NotNull(message = "Tip dogadjaja je obavezan.")
    private EventType type;

    @NotNull(message = "Datum i vreme dogadjaja su obavezni.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime date;

    @NotNull(message = "Oznaka da li je dogadjaj redovan je obavezna.")
    private Boolean regular;

    @NotNull(message = "Oznaka da li je ulaz besplatan je obavezna.")
    private Boolean freeEntry;

    /** Obavezna kada dogadjaj nije besplatan; provera je u servisu. */
    @DecimalMin(value = "0.0", inclusive = false, message = "Cena mora biti veca od nule.")
    @Digits(integer = 8, fraction = 2, message = "Cena nije u ispravnom formatu.")
    private BigDecimal price;

    /** Obavezna pri kreiranju; pri izmeni je opciona (bez nje slika ostaje ista). */
    private MultipartFile image;
}
