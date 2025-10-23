package ch.semafor.gendas.dao.mongo.custom;

import org.springframework.context.annotation.Profile;

import java.util.List;

@Profile("mongo")
public interface ElementTypeRepositoryMongoCustom {

    List<String> findAllNames();
}
