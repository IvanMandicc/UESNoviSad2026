package rs.ftn.uns.novisad.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Ukljucuje skeniranje @ConfigurationProperties klasa i asinhrono izvrsavanje,
 * da slanje mejla ne blokira odgovor korisniku.
 */
@Configuration
@ConfigurationPropertiesScan("rs.ftn.uns.novisad")
@EnableAsync
public class AppProperties {
}
