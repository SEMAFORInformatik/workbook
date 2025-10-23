package ch.semafor.gendas.dao.mongo.custom;

import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Profile("mongo")
@Service("groupRepository")
public class GroupRepositoryMongoImpl implements GroupRepositoryMongoCustom {

    private static final Logger logger = LoggerFactory.getLogger(GroupRepositoryMongoImpl.class);

    @Autowired
    MongoTemplate mt;

    public List<Group> findAll() {
        Query q = new Query();  // there could be some better way
        q.fields().include("groups");
        Set<Group> g = new HashSet<Group>();
        for (Owner o : mt.find(q, Owner.class)) {
            g.addAll(o.getGroups());
        }
        return new ArrayList<Group>(g);

    }

    /**
     * Searches if given group exists and returns it if true
     *
     * @param name Name of the group
     * @return Searched group if exist
     */
    public Group findByName(String name) {
        Query q = new Query();  // there could be some better way
        q.fields().include("groups");
        for (Owner o : mt.find(q, Owner.class)) {
            if (o.getGroups().contains(new Group(name))) {
                // There is a owner with the given group
                logger.debug("Found owner: {} with group: {}", o.getUsername(), name);
                return new Group(name);
            }
        }
        return null;
    }

}
