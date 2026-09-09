package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [K5] Telo zahteva za ostavljanje utiska.
 * <p>
 * Ocene su opcione pojedinacno - nije neophodno oceniti svaku stavku - ali
 * bar jedna mora biti data, jer se "prilikom pisanja utiska formira ocena mesta".
 */
public record CreateReviewDto(

        @NotNull(message = "Izaberite dogadjaj na koji se utisak odnosi.")
        Long eventId,

        @Min(value = 1, message = "Ocena nastupa mora biti izmedju 1 i 10.")
        @Max(value = 10, message = "Ocena nastupa mora biti izmedju 1 i 10.")
        Integer performance,

        @Min(value = 1, message = "Ocena zvuka i svetla mora biti izmedju 1 i 10.")
        @Max(value = 10, message = "Ocena zvuka i svetla mora biti izmedju 1 i 10.")
        Integer soundAndLight,

        @Min(value = 1, message = "Ocena prostora mora biti izmedju 1 i 10.")
        @Max(value = 10, message = "Ocena prostora mora biti izmedju 1 i 10.")
        Integer space,

        @Min(value = 1, message = "Ukupan utisak mora biti izmedju 1 i 10.")
        @Max(value = 10, message = "Ukupan utisak mora biti izmedju 1 i 10.")
        Integer overall,

        /** Opcioni komentar uz utisak. */
        @Size(max = 2000, message = "Komentar moze imati najvise 2000 karaktera.")
        String comment
) {
}
