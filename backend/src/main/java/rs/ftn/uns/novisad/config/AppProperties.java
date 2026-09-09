package rs.ftn.uns.novisad.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/** Ukljucuje skeniranje @ConfigurationProperties klasa. */
@Configuration
@ConfigurationPropertiesScan("rs.ftn.uns.novisad")
public class AppProperties {
}
