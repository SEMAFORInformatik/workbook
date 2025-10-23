package ch.semafor.intens.ws;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ComponentScan(basePackages = {"ch.semafor.intens.ws"})
@EntityScan("ch.semafor.gendas.model")
@EnableJpaRepositories("ch.semafor.gendas.dao")
@EnableAutoConfiguration
@SpringBootConfiguration
public class Configuration {
}
