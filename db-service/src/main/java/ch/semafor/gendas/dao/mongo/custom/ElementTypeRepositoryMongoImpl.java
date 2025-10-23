package ch.semafor.gendas.dao.mongo.custom;

import ch.semafor.gendas.model.ElementType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Profile("mongo")
@Repository
public class ElementTypeRepositoryMongoImpl implements ElementTypeRepositoryMongoCustom {

    @Autowired
    MongoTemplate mt;

    /**
     * find list of all names
     */
    public List<String> findAllNames() {
        Class<ElementType> clzz = ElementType.class;
        Query q = new Query();
        q.fields().include("name");
        List<String> res = new ArrayList<String>();
        for (ElementType t : mt.find(q, clzz, "types")) {
            res.add(t.getName());
        }
        //logger.debug("MongoDB return map size {}", res.size());
        return res;
    }

}
