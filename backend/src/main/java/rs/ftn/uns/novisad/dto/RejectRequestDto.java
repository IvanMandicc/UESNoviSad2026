package rs.ftn.uns.novisad.dto;

import jakarta.validation.constraints.Size;

/** [A1] Opciono obrazlozenje pri odbijanju zahteva. */
public record RejectRequestDto(
        @Size(max = 500, message = "Obrazlozenje moze imati najvise 500 karaktera.")
        String reason
) {
}
