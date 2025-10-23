package ch.semafor.gendas.dao.mongo;

import ch.semafor.gendas.dao.OwnerRepository;
import ch.semafor.gendas.model.Owner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;

@Profile("mongo")
public interface OwnerRepositoryMongo extends MongoRepository<Owner, String>, OwnerRepository {


}
