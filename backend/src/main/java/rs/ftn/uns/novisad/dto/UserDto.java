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
        String role
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getCity(),
                u.getPhone(),
                u.getRole().name()
        );
    }
}
