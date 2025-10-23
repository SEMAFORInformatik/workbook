package ch.semafor.intens.ws;

import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "ch.semafor.gendas.dao")
@Profile("jpa")
@Configuration
public class ApplicationJpa {
}
