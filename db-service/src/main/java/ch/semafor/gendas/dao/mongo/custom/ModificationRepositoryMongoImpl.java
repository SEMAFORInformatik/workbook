package ch.semafor.gendas.dao.mongo.custom;

import ch.semafor.gendas.model.MapModification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Profile("mongo")
@Repository
public class ModificationRepositoryMongoImpl implements ModificationRepositoryMongoCustom {

    @Autowired
    MongoTemplate mt;

    public List<MapModification> findAllOfElement(Long id, Pageable pageable) {
        if (pageable == null) {
            return mt.find(query(where("elementRef").is(id)), MapModification.class);
        }
        return mt.find(query(where("elementRef").is(id)).with(pageable), MapModification.class);
    }

    public void deleteAllOfElement(Long id) {
        mt.remove(query(where("elementRef").is(id)), MapModification.class);
    }

}
