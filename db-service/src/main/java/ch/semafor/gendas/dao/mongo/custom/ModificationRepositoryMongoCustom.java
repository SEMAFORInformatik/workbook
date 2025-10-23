package ch.semafor.gendas.dao.mongo.custom;

import ch.semafor.gendas.model.MapModification;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Profile("mongo")
public interface ModificationRepositoryMongoCustom {

    List<MapModification> findAllOfElement(Long id, Pageable pageable);

    void deleteAllOfElement(Long id);
}
