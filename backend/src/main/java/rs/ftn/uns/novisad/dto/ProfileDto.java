package rs.ftn.uns.novisad.dto;

import java.util.List;

/**
 * [K10] Profil korisnika: podaci, spisak svih utisaka koje je ostavio
 * i mesta na kojima je menadzer.
 */
public record ProfileDto(
        UserDto user,
        List<ReviewDto> reviews,
        List<LocationDto> managedLocations
) {
}
