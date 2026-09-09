package rs.ftn.uns.novisad.dto;

import rs.ftn.uns.novisad.model.Manages;

import java.time.Instant;

/** [A2] Menadzer dodeljen mestu. */
public record ManagerDto(
        Long userId,
        String email,
        String firstName,
        String lastName,
        Instant assignedAt
) {
    public static ManagerDto from(Manages manages) {
        return new ManagerDto(
                manages.getUser().getId(),
                manages.getUser().getEmail(),
                manages.getUser().getFirstName(),
                manages.getUser().getLastName(),
                manages.getAssignedAt()
        );
    }
}
