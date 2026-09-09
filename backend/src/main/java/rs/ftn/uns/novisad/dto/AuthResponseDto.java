package rs.ftn.uns.novisad.dto;

/** [K2] Odgovor na uspesnu prijavu. */
public record AuthResponseDto(
        String token,
        String tokenType,
        long expiresIn,
        UserDto user
) {
    public static AuthResponseDto of(String token, long expiresInSeconds, UserDto user) {
        return new AuthResponseDto(token, "Bearer", expiresInSeconds, user);
    }
}
