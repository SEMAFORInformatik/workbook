package ch.semafor.gendas.service;


import ch.semafor.gendas.dao.mongo.ElementRepositoryMongo;
import ch.semafor.gendas.dao.mongo.ElementTypeRepositoryMongo;
import ch.semafor.gendas.dao.mongo.ModificationRepositoryMongo;
import ch.semafor.gendas.dao.mongo.OwnerRepositoryMongo;
import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.*;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Profile("mongo")
@Service("elementService")
public class ElementServiceMongo implements ElementService {
    private static final Logger logger = LoggerFactory.getLogger(ElementServiceMongo.class);
    private final Map<String, ElementType> elementTypes = new HashMap<>();
    @Autowired
    private ElementRepositoryMongo elementRepository;
    @Autowired
    private OwnerRepositoryMongo ownerRepository;
    @Autowired
    private ElementTypeRepositoryMongo elementTypeRepository;
    @Autowired
    private ModificationRepositoryMongo modificationDao;

    private ElementType getElementType(final String name, final String idName,
                                       final String idVersion) {
        if (elementTypeRepository.existsByName(name)) {
            logger.debug("{} created? {}", name, getElementType(name).isCreated());
            return getElementType(name);
        }
        ElementType et = elementTypes.get(name);
        if (et != null) {
            et.setCreated();
        } else {
            logger.debug("Created Element Type {}", name);
            et = new ElementType(name);
            if (idName != null && idName.length() > 0) {
                et.setBeanId(idName);
            }
            if (idVersion != null && idVersion.length() > 0) {
                et.setBeanVersionId(idVersion);
            }
            elementTypes.put(name, et);
        }
        return et;
    }

    private ElementType create(final String typename, final List<Map<String, Object>> typedef,
                               final String idName, final String versionName) {
        logger.debug("Begin of create({})", typename);
        final ElementType elementType = getElementType(typename, idName, versionName);

        if (!elementType.isCreated()) { // prevent infinite recursion
            // New Type
            logger.debug("Create new type {}", typename);
            elementType.setCreated();
        }

        for (Map<String, Object> propdef : typedef) {
            if (!propdef.containsKey("props")) {
                String propname = (String) propdef.get("name");
                String unit = (String) propdef.get("unit");
                String type = (String) propdef.get("type");
                logger.debug("** Property: name={}, type={}, unit={}", propname, type, unit);
                PropertyType propType = elementType.getPropertyType(propname);
                if (propType == null) {
                    elementType.add(new PropertyType(propname, PropertyType.Type.get(type), unit));
                }
            } else {
                String refname = (String) propdef.get("name");
                String name = (String) propdef.get("type");
                logger.debug("** Reference: name={}, type={}", refname, name);
                ElementType et =
                        create(name, (List<Map<String, Object>>) propdef.get("props"), idName, versionName);
                elementTypeRepository.save(et);
                elementType.addReference(refname, et);
            }
        }
        logger.debug("End of create({})", typename);
        return elementType;
    }

    @Override
    public List<ElementType> getAllElementTypes() {
        return elementTypeRepository.findAll();
    }

    @Override
    @Transactional
    public ElementType createElementType(String typename, List<Map<String, Object>> typedef,
                                         String idName, String idVersion) {
        logger.debug("=== Create Element Type {} ===", typename);
        ElementType et = create(typename, typedef, idName, idVersion);
        return elementTypeRepository.save(et);
    }

    @Override
    @Transactional
    public Map<String, Object> save(Map<String, Object> map, String type, String username,
                                    String changeComment) throws UsernameNotFoundException, CoreException {
        Map<String, Object> diff = getModifiedProperties(map);
        if (diff != null && diff.isEmpty())
            return map;
        Map<String, Object> newMap = elementRepository.save(type, map);
        MapModification m = new MapModification((Long) newMap.get("id"),
                ownerRepository.findByUsername(username), changeComment, diff);
        modificationDao.save(m);
        return newMap;
    }

    @Override
    public List<Map<String, Object>> findByType(final String type, final String owner,
                                                final List<String> pnames, final Map<String, Object> searchargs,
                                                final Map<String, Map<String, Object>> childargs, int page, int pagesize,
                                                Map<String, Integer> sortmap, boolean latestRevision) {
        return findByType(type, owner, pnames, searchargs, childargs, page, pagesize, sortmap, latestRevision, null);
    }
    /**
     *
     */
    // TODO: implement latestRevision
    @Override
    public List<Map<String, Object>> findByType(final String type, final String owner,
                                                final List<String> pnames, final Map<String, Object> searchargs,
                                                final Map<String, Map<String, Object>> childargs, int page, int pagesize,
                                                Map<String, Integer> sortmap, boolean latestRevision, Date changedSince) {

        boolean ignorecase = false;
        if (searchargs != null && searchargs.containsKey("ignorecase")) {
            ignorecase = (Boolean) searchargs.get("ignorecase");
            searchargs.remove("ignorecase");
        }

        if (pagesize < 1) {
            pagesize = Integer.MAX_VALUE;
            page = 0;
        }
        Pageable pageable = null;
        if (sortmap != null) {
            List<Sort.Order> orders = new ArrayList<Sort.Order>();
            for (String key : sortmap.keySet()) {
                Sort.Direction d = sortmap.get(key) > 0 ? Sort.Direction.ASC : Sort.Direction.DESC;
                Sort.Order order = new Sort.Order(d, key);
                if (ignorecase) {
                    order.ignoreCase();
                }
                orders.add(order);
            }
            pageable = PageRequest.of(page, pagesize, Sort.by(orders));
        } else {
            pageable = PageRequest.of(page, pagesize, Sort.by(Sort.Direction.DESC, "_id"));
        }
        List<Map<String, Object>> r =
                elementRepository.find(type, owner, pnames, searchargs, childargs, pageable);
        return r;
    }

    @Override
    public Map<String, Object> getModifiedProperties(Map<String, Object> elementMap) {
        logger.debug("element map {}", elementMap.keySet().size());
        if (elementMap.get("id") != null) {
            Long id = Long.valueOf(((Number) elementMap.get("id")).longValue());
            final Map<String, Object> orig = getElementMap(id, false);
            return Maps.diff(orig, elementMap);
        }
        return null; // new element
    }

    @Override
    public boolean checkVersion(final Long id, final Long version, String type) {
        if (id == null || version == null) {
            logger.warn("missing id or version");
            return false;
        }
        Long existingVersion = elementRepository.findVersion(id, type);
        if (existingVersion == null) {
            logger.warn("missing version in element {}", id);
            return false;
        }
        logger.info("Existing {} Other {}", existingVersion, version);
        return existingVersion.equals(version);
    }

    @Override
    public Map<String, Object> getElementMap(Long id) {
        return getElementMap(id, true);
    }

    @Override
    public Map<String, Object> getElementMap(Long id, boolean withDbInfos) {
        String type = findTypeOfElement(id);
        if (type == null) {
            logger.warn("type not found for element {}", id);
            return null;
        }
        logger.debug("found type {}", type);

        // withDbInfos
        List<String> pnames = null;
        if (!withDbInfos){
            pnames = new ArrayList<>();
        }
        Map<String, Object> m = elementRepository.findById(id, pnames, type);

        if (m.get("id") != null)
            return m;
        // not found
        return null;
    }

    @Override
    public String getTypeOfElement(Long id) {
        String type = findTypeOfElement(id);
        if (type == null) {
            logger.warn("type not found for element {}", id);
            return null;
        }
        logger.debug("found type {}", type);
        return type;
    }

    public int changeGroupInElements(Group oldGroup, Group newGroup) {
        int changes = 0;

        List<Map<String, Object>> elements = elementRepository.findByGroup(oldGroup);
        for (Map<String, Object> element : elements) {
            logger.debug("Change Element {} from old group {} to new group {}", element.get("id"),
                    newGroup.getName(), oldGroup.getName());
            element.put("group", newGroup.getName());
            try {
                elementRepository.save((String) element.get("_type"), element);
            } catch (CoreException e) {
                logger.warn("Element could not be saved: {}", e.getMessage());
            }
            element.remove("_type");
            changes++;
        }
        return changes;
    }

    @Override
    public Map<String, Object> getElement(Long id, Long mod_id) throws CoreException {
        String type = findTypeOfElement(id);
        if (type == null) {
            return null;
        }
        Map<String, Object> last = new HashMap<String, Object>(elementRepository.findById(id, type));
        List<MapModification> modifications = modificationDao.findAllOfElement(id, null);
        ListIterator<MapModification> miter = modifications.listIterator(modifications.size());
        while (miter.hasPrevious()) {
            MapModification m = miter.previous();
            if (m.getId().equals(mod_id))
                return last;
            logger.debug("merging mod {} (not yet {})", m.getId(), mod_id);
            last = Maps.merge(last, m.getOld());
        }
        return last;
    }

    @Override
    public ElementType getElementType(String type) {
        try {
            return elementTypeRepository.findByName(type);
        } catch (IncorrectResultSizeDataAccessException ex) {
            throw new DataIntegrityViolationException("Non Unique Element Type '" + type + "'");
        }
    }

    @Override
    public Map<Long, Pair<Long, Long>> getOriginalIdsBeforeImport() {
        var mods = modificationDao.findByCommentLike(IMPORT_PREFIX + "%");
        var idMap = new HashMap<Long, Pair<Long, Long>>();

        for (var mod : mods) {
            var stringId = mod.getComment().replace(IMPORT_PREFIX, "").split(",");
            var pair = new ImmutablePair<>(
                Long.valueOf(stringId[0]),
                Long.valueOf(stringId[1])
            );
            idMap.put(mod.getElementRef(), pair);
        }
        return idMap;
    }

    @Override
    public List<Map<String, Object>> getAllChangedSinceImport() {
        return modificationDao.findByCommentNotLike(IMPORT_PREFIX + "%").stream()
            .map(m -> getElementMap(m.getElementRef())).distinct().collect(Collectors.toList());
    }

    @Override
    public Modification getFirstModification(Long elementId) {
        List<Modification> mods = getModifications(elementId, 0, 1, Sort.Direction.ASC);
        return mods.isEmpty() ? null : mods.get(0);
    }

    @Override
    public Modification getLatestModification(Long elementId) {
        List<Modification> mods = getModifications(elementId, 0, 1, Sort.Direction.DESC);
        return mods.isEmpty() ? null : mods.get(0);
    }

    @Override
    public List<Modification> getModifications(Long elementId, int page, int pagesize, Sort.Direction sortDirection) {
        if (pagesize < 1) {
            pagesize = Integer.MAX_VALUE;
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, pagesize,
                Sort.by(sortDirection, "_id"));

        List<Modification> mlist = new ArrayList<Modification>(modificationDao.findAllOfElement(elementId, pageable));
        return mlist;
    }

    @Override
    public Map<String, Object> getElement(Long id, List<String> pnames) {
        String type = findTypeOfElement(id);
        if (type == null) {
            logger.warn("type not found for element {}", id);
            return null;
        }
        logger.debug("found type {}", type);
        Map<String, Object> m = elementRepository.findById(id, pnames, type);
        if (m.get("id") != null)
            return m;
        // not found
        return null;
    }

    @Override
    @Transactional
    public void deleteElement(Long id) {
        String type = findTypeOfElement(id);
        if (type == null) {
            logger.warn("type not found for element {}", id);
        }
        modificationDao.deleteAllOfElement(id);
        elementRepository.deleteElement(id, type);
    }

    public List<Group> getAllGroups() {
        return elementRepository.findAllGroups();
    }

    /**
     * Helper method to get type of element
     *
     * @param id id of the element
     * @return type of the element
     */
    private String findTypeOfElement(Long id) {
        // must loop over all element types (collections)
        for (String type : elementTypeRepository.findAllNames()) {
            logger.debug("find by id {} type {}", id, type);
            Map<String, Object> e = elementRepository.findById(id, type);
            if (e.get("id") != null) {
                return type;
            }
        }
        logger.warn("Element {} not found", id);
        return null;
    }
}
