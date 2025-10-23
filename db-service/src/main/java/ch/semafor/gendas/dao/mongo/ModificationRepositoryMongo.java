package ch.semafor.gendas.dao.mongo;

import ch.semafor.gendas.dao.mongo.custom.ModificationRepositoryMongoCustom;
import ch.semafor.gendas.model.MapModification;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;


@Profile("mongo")
public interface ModificationRepositoryMongo extends MongoRepository<MapModification, Long>, ModificationRepositoryMongoCustom {

  List<MapModification> findByCommentNotLike(String comment);
  List<MapModification> findByCommentLike(String comment);
}
