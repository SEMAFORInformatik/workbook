package ch.semafor.gendas.dao.mongo;

import ch.semafor.gendas.dao.GroupRepository;
import ch.semafor.gendas.dao.mongo.custom.GroupRepositoryMongoCustom;
import org.springframework.context.annotation.Profile;

@Profile("mongo")
public interface GroupRepositoryMongo extends GroupRepository, GroupRepositoryMongoCustom {

}
