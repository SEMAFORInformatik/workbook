package ch.semafor.intens.ws;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories("ch.semafor.gendas.dao.mongo")
@Profile("mongo")
@Configuration
@Import({
  MongoAutoConfiguration.class, 
  MongoDataAutoConfiguration.class
})
public class ApplicationMongo {
}
