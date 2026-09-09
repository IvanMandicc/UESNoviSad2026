package rs.ftn.uns.novisad.dto;

import rs.ftn.uns.novisad.model.User;

/** Podaci o prijavljenom korisniku (bez lozinke). */
public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String city,
        String phone,
        String role,
        /** [K10] Adresa slike profila; null kada korisnik nema sliku. */
        String imageUrl
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getCity(),
                u.getPhone(),
                u.getRole().name(),
                u.getImageKey() == null ? null : "/api/users/" + u.getId() + "/image"
        );
    }
}
