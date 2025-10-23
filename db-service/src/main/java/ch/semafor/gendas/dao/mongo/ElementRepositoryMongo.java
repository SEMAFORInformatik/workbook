package ch.semafor.gendas.dao.mongo;

import ch.semafor.gendas.dao.mongo.custom.ElementRepositoryMongoCustom;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Map;

@Profile("mongo")
public interface ElementRepositoryMongo extends MongoRepository<Map<String, Object>, Long>, ElementRepositoryMongoCustom {
}
