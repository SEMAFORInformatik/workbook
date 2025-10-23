package ch.semafor.gendas.dao.mongo.custom;

import ch.semafor.gendas.dao.mongo.ModificationRepositoryMongo;
import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.gendas.search.SearchOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Profile("mongo")
@Repository
public class ElementRepositoryMongoImpl implements ElementRepositoryMongoCustom {

    private static final Logger logger = LoggerFactory.getLogger(ElementRepositoryMongoImpl.class);

    @Autowired
    MongoTemplate mt;

    @Autowired
    private ModificationRepositoryMongo modificationDao;

    /**
     * save map
     *
     * @param type elementtype of the obj
     * @param map  collection of all properties
     * @return map
     * @throws CoreException
     */
    public Map<String, Object> save(String type, Map<String, Object> map) throws CoreException {
        Number version = map.containsKey("version") ?
                (Number) map.get("version") : null;

        // must eventually check version for up-to-date-conflicts
        if (version != null && map.containsKey("id")) {
            checkVersion((Long) map.get("id"), version.longValue(), type);
        }
        if (version != null) {
            map.put("version", version.longValue() + 1L);
        }
        map = mt.save(map, type);
        return findById((Long) map.get("_id"), type);
    }

    public void checkVersion(Long id, Long version, String collection) throws CoreException {
        Long existingVersion = findVersion(id, collection);
        if (existingVersion != null && !existingVersion.equals(version)) {
            throw new CoreException("Up-to-date Conflict");
        }
    }

    public Long findVersion(Long id, String collection) {
        if (id == null)
            return null;
        Query query = new Query(Criteria.where("_id").is(id));
        query.fields().include("version");
        Map<String, Object> m = mt.findOne(query, Map.class, collection);
        if (m == null)
            return null;
        return (Long) m.get("version");
    }

    /**
     * find list of maps
     *
     * @param type
     * @param owner
     * @param pnames
     * @param searchargs
     * @param childargs
     * @return list of maps
     */
    public List<Map<String, Object>> find(final String type, final String owner,
                                          final List<String> pnames,
                                          final Map<String, Object> searchargs,
                                          final Map<String, Map<String, Object>> childargs,
                                          Pageable pageable) {

        Criteria crit = null;
        if (owner != null && !owner.isEmpty()) {
            crit = Criteria.where("owner").regex(owner);
        }
        if (searchargs != null) {
            for (String key : searchargs.keySet()) {
                SearchOp op = (SearchOp) searchargs.get(key);
                if (key.equals("id")) {
                    key = "_id";
                }
                if (crit != null) {
                    crit = crit.and(key);
                } else {
                    crit = Criteria.where(key);
                }
                crit = op.setCriteria(crit);
            }
        }
        if (childargs != null) {
            for (String key : childargs.keySet()) {
                Map<String, Object> subargs = childargs.get(key);
                for (String subkey : subargs.keySet()) {
                    SearchOp op = (SearchOp) subargs.get(subkey);
                    if (crit != null) {
                        crit = crit.and(key + "." + subkey);
                        logger.debug(" and {}", key + "." + subkey);
                    } else {
                        crit = Criteria.where(key + "." + subkey);
                        logger.debug(" where {}", key + "." + subkey);
                    }
                    op.setCriteria(crit);
                }
            }
        }
        if (crit != null) {
            logger.debug("MongoDB find type {} crit {}",
                    type, crit);

            if (searchargs != null) {
                logger.debug("   searchargs {}",
                        searchargs);
            }
            if (childargs != null) {
                logger.debug("    childargs {}",
                        childargs);
            }
        } else {
            logger.debug("MongoDB find all of type {}", type);
        }
        Class clzz = Map.class;
        Query q = crit == null ? new Query() : new Query(crit);

        if (pnames != null && !pnames.isEmpty()) {
            for (String p : pnames) {
                q.fields().include(p);
            }
        }
        if (pageable != null) {
            q.with(pageable);
            logger.debug("limit {} skip {}", pageable.getPageSize(), pageable.getPageNumber());
        }
        logger.debug("Query: {}", q);
        long start = System.currentTimeMillis();
        List<Map<String, Object>> found = mt.find(q, clzz, type);
        logger.debug("elapsed time: {}", (System.currentTimeMillis() - start));
        if (found != null) {
            logger.debug("found objects {}", found.size());
        }
        List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
        for (Map m : found) {
            Map<String, Object> r = null;
            updateMap(m, type, pnames);
            r = new HashMap<String, Object>(m);
            res.add(r);
        }
        if (res != null) {
            logger.debug("MongoDB return map size {}", res.size());
        }
        return res;
    }

    public Map<String, Object> findById(Long id, String type) {
        if (type == null)
            return new HashMap<String, Object>();

        Map<String, Object> m = mt.findById(id, Map.class, type);
        updateMap(m, type, null);
        if (m == null)
            m = new HashMap<String, Object>();
        return m;
    }

    public Map<String, Object> findById(Long id, List<String> pnames,
                                        String type) {

        Criteria crit = Criteria.where("_id");
        SearchOp op = new SearchEq<Long>(id.longValue());
        crit = op.setCriteria(crit);
        Class clzz = Map.class;
        Query q = crit == null ? new Query() : new Query(crit);

        if (pnames != null && !pnames.isEmpty()) {
            for (String p : pnames) {
                q.fields().include(p);
            }
        }
        Map<String, Object> res = mt.findOne(q, Map.class, type);
        updateMap(res, type, pnames);
        if (res != null) {
            logger.debug("found object {}", id);
            logger.debug("MongoDB return map size {}", res.size());
        }
        return res;
    }

    public List<Map<String, Object>> findByGroup(Group group) {
        List<Map<String, Object>> elements = new ArrayList<>();

        for (String type : findTopLevelTypes()) {
            Query query = new Query(Criteria.where("group").is(group.getName()));
            List<Map> maps = mt.find(query, Map.class, type);
            for (Map map : maps) {
                // This is a temporary attribute -> gets removed in ElementServiceMongo.changeGroupInElements
                map.put("_type", type);
                elements.add(map);
            }
            logger.debug("Got {} elements in collection: {} to change group", maps.size(), type);

        }
        return elements;

    }

    /**
     * Searches all groups in elements-collections
     *
     * @return List of every found groups in all elements
     */
    public List<Group> findAllGroups() {
        Set<Group> groups = new HashSet<>();
        for (String type : findTopLevelTypes()) {
            List<Map> maps = mt.findAll(Map.class, type);
            for (Map map : maps) {
                if (map.get("group") != null) {
                    groups.add(new Group((String) map.get("group")));
                }
            }
        }
        return new ArrayList<>(groups);
    }

    private List<String> findTopLevelTypes() {
        // Get all collections with the name of an elementType to overwrite 'group' attribute
        Set<String> collections = mt.getCollectionNames();
        List<ElementType> elementTypes = mt.findAll(ElementType.class, "types");
        List<String> types = new ArrayList<>();
        for (ElementType type : elementTypes) {
            if (collections.contains(type.getName())) {
                types.add(type.getName());
            }
        }
        logger.debug("Found {} types: {}", types.size(), types);
        return types;
    }

    public void deleteElement(final Long id, final String type) {
        Query query = new Query(Criteria.where("_id").is(id));
        mt.remove(query, type);
    }

    private void updateMap(Map<String, Object> map, String type, List<String> pnames) {
        // add type and last modification properties to map, if wanted

        // no id in map
        if (map == null || !map.keySet().contains("id")) {
            return;
        }

        if (pnames == null || pnames.contains("type")){
            map.put("type", type);
        }

        // no last modification properties wanted
        if (!(pnames == null ||
              pnames.contains("modcomment") ||
              pnames.contains("changed") ||
              pnames.contains("changer") ||
              pnames.contains("changername"))) {
            return;
        }

        // load last modification
        var id = map.get("id");
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id"));
        var mods = modificationDao.findAllOfElement((Long) id, pageable);
        if (mods.size() == 0) {
            return;
        }
        var lastMod = mods.get(mods.size() - 1);

        // add wanted last modification properties to map
        /* modification attribute comment */
        if (pnames == null || pnames.contains("modcomment")) {
            if (lastMod.getComment() != null){
                map.put("modcomment", lastMod.getComment());
            }
        }
        /* modification attribute timestamp */
        if (pnames == null || pnames.contains("changed")) {
            map.put("changed", lastMod.getTimestamp());
        }
        /* modification attribute user (username) */
        if (pnames == null || pnames.contains("changer")) {
            if (lastMod.getUser() != null) {
                map.put("changer", lastMod.getUser().getUsername());
            }
        }
        /* modification attribute user (full name) */
        if (pnames == null || pnames.contains("changername")) {
            if (lastMod.getUser() != null) {
                map.put("changername", lastMod.getUser().getFullName());
            }
        }
    }


}
