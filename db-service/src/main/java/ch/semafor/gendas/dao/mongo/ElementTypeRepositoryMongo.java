package ch.semafor.gendas.dao.mongo;

import ch.semafor.gendas.dao.mongo.custom.ElementTypeRepositoryMongoCustom;
import ch.semafor.gendas.model.ElementType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;


@Profile("mongo")
public interface ElementTypeRepositoryMongo extends MongoRepository<ElementType, Long>, ElementTypeRepositoryMongoCustom {

    ElementType findByName(String name);

    boolean existsByName(String name);
}
