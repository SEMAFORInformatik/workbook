package ch.semafor.gendas.dao.mongo.custom;

import ch.semafor.gendas.model.Group;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Profile("mongo")
public interface GroupRepositoryMongoCustom {

    List<Group> findAll();

    Group findByName(String name);
}
