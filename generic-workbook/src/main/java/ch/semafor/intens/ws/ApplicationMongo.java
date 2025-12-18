package ch.semafor.intens.ws;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories("ch.semafor.gendas.dao.mongo")
@Profile("mongo")
@Configuration
@Import({
  MongoAutoConfiguration.class, 
  DataMongoAutoConfiguration.class
})
public class ApplicationMongo {
}
