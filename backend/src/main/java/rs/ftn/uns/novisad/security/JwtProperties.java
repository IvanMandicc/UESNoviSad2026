package rs.ftn.uns.novisad.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Podesavanja JWT tokena (app.jwt.* u application.yml). */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds,
        String issuer
) {
}
