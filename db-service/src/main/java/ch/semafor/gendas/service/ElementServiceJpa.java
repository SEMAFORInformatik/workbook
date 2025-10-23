package ch.semafor.gendas.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.semafor.gendas.dao.jpa.ElementRepositoryJpa;
import ch.semafor.gendas.dao.jpa.ElementTypeRepositoryJpa;
import ch.semafor.gendas.dao.jpa.GroupRepositoryJpa;
import ch.semafor.gendas.dao.jpa.ModificationRepositoryJpa;
import ch.semafor.gendas.dao.jpa.OwnerRepositoryJpa;
import ch.semafor.gendas.dao.jpa.PropertyTypeRepositoryJpa;
import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.ElementCreationException;
import ch.semafor.gendas.exceptions.ElementTypeCreationException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.model.ElementRefList;
import ch.semafor.gendas.model.ElementRefs;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Modification;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.TableModification;
import ch.semafor.gendas.repldbfunc.MapComparator;
import jakarta.persistence.PersistenceException;

@Transactional(readOnly = true)
@Service("elementService")
@Profile("jpa")
public class ElementServiceJpa implements ElementService {
    private static final Logger logger = LoggerFactory.getLogger(ElementServiceJpa.class);
    @Autowired
    private ElementRepositoryJpa elementRepository;
    @Autowired
    private ElementTypeRepositoryJpa elementTypeRepositoryJpa;
    @Autowired
    private ModificationRepositoryJpa modificationRepositoryJpa;
    @Autowired
    private OwnerRepositoryJpa ownerRepository;
    @Autowired
    private GroupRepositoryJpa groupRepository;
    @Autowired
    private PropertyTypeRepositoryJpa propertyTypeRepositoryJpa;

    @Transactional
    public ElementType createElementType(final String typename, final List<Map<String, Object>> typedef, String idName, String versionName) {
        try {
            final ElementType t = new ElementTypeCreator(elementTypeRepositoryJpa,
                propertyTypeRepositoryJpa).create(typename, typedef, idName, versionName);
            logger.info("Saving " + t.getName());
            logger.debug(t.toString());
            elementTypeRepositoryJpa.save(t);
            return t;
        } catch (DataIntegrityViolationException ex) {
            throw new ElementTypeCreationException("type " + typename + " " +
                    ex.getMessage());
        }
    }

    @Override
    public List<ElementType> getAllElementTypes() {
        return elementTypeRepositoryJpa.findAll();
    }

    private Element createElement(Map<String, ?> map, String type, String username) throws ElementCreationException, CoreException, UsernameNotFoundException {
        final Owner owner = (username != null) ? ownerRepository.findByUsername(username) : null;
        final ElementType elType = elementTypeRepositoryJpa.findByName(type);
        if (elType == null) {
            throw new ElementCreationException("Invalid element type: " + type);
        }
        if (map == null) {
            throw new ElementCreationException("Content must not be null");
        }
        final ElementCreator creator = new ElementCreator(elementTypeRepositoryJpa,
                elementRepository,
                ownerRepository, groupRepository,
                propertyTypeRepositoryJpa);
        logger.debug("about to create element tree for map of {}", type);
        final Element newElement = creator.create(map, elType);
        creator.resetMaps();

        Element element = null;
        Long id = newElement.getId();
        if (id != null && id.compareTo(Long.valueOf(0L)) > 0) {
            element = elementRepository.findById(id).orElse(null);
        }
        if (element == null) {
            logger.debug("Element is new -> no id found: Create new Element");
            element = new Element(newElement.getElementType());
        }
        List<Element> knownElements = new ArrayList<Element>();
        knownElements.add(element);
        logger.debug("Assign new element: {}", newElement);
        element.assign(newElement, knownElements, elementRepository);
        logger.debug("Assigned Element: {}", element);
        if (logger.isDebugEnabled()) {
            element.print(0, "Element after assign() before save()");
        }
        try {
            if (element.getOwner() == null && owner != null) {
                element.setOwner(owner);
            }
            return element;
        } catch (PersistenceException e) {
            throw new CoreException("Constrained violation: " + e.getMessage() + " for " + element.getElementType() + " of " + owner);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> save(Map<String, Object> map, String type,
                                    String username, String changeComment)
            throws UsernameNotFoundException, CoreException {

        logger.debug("save element {} .", map);
        Map<String, Object> diff = getModifiedProperties(map);
        if (diff != null && diff.isEmpty())
            return map;

        if (diff != null) {
            logger.debug("diff {} .", diff);
        }
        try {
            Element element = createElement(map, type, username);
            element.getLastModification().setComment(changeComment);
            element.getLastModification().setUser(ownerRepository.findByUsername(username));
            logger.debug("Before elementRepository save {}", element);
            element = elementRepository.save(element);
            element.setVersion(element.getVersion() + 1);

            // //creator.setMatchingIdsAndVersions(map, element, null);
            logger.debug("map of type {} persisted.", type);
            logger.debug("return element {} .", element);
            return element.toMap();
        } catch (PersistenceException e) {
            throw new CoreException("Constrained violation: " + e.getMessage()
                    + " for " + type + " of " + username);
        } catch (ElementCreationException e) {
            throw new CoreException("Cannot create element " + e.getMessage());
            // e.printStackTrace();
        }
    }

    /**
     *
     */
    @Override
    public List<Map<String, Object>> findByType(final String type, final String owner,
                                                final List<String> pnames,
                                                final Map<String, Object> searchargs,
                                                final Map<String, Map<String, Object>> childargs,
                                                int page, int pagesize,
                                                Map<String, Integer> sortmap, boolean latestRevision) {
        return findByType(type, owner, pnames, searchargs, childargs, page, pagesize, sortmap, latestRevision, null);
    }

    @Override
    public List<Map<String, Object>> findByType(final String type, final String owner,
                                                final List<String> pnames,
                                                final Map<String, Object> searchargs,
                                                final Map<String, Map<String, Object>> childargs,
                                                int page, int pagesize,
                                                Map<String, Integer> sortmap, boolean latestRevision, Date changedSince) {
        logger.debug("searching elements of type {}, pnames = {}", type, pnames);
        boolean ignorecase = false;
        if (searchargs != null && searchargs.containsKey("ignorecase")) {
            ignorecase = (Boolean) searchargs.get("ignorecase");
            searchargs.remove("ignorecase");
        }

        if (pagesize < 1) {
            pagesize = Integer.MAX_VALUE;
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, pagesize,
                    Sort.by(Sort.Direction.DESC, "id"));

        if (sortmap != null) {
            var orders = new ArrayList<Order>();
            // there are keys we can always sort on
            var alwaysDBSortable = Arrays.asList("changed", "id", "index", "owner", "ownername");
            for (var key : alwaysDBSortable) {
                var order = sortmap.remove(key);
                if (order != null) {
                    var direction = order == 1 ? Sort.Direction.ASC : Sort.Direction.DESC;
                    orders.add(new Order(direction, key));
                }
            }
            if (!sortmap.isEmpty()) {
                // we can only sort on one arbitrary element property on the db
                var firstKey = sortmap.keySet().iterator().next();
                var order = sortmap.remove(firstKey);
                if (order != null) {
                    var direction = order == 1 ? Sort.Direction.ASC : Sort.Direction.DESC;
                    orders.add(new Order(direction, firstKey));
                }
            }
            // remove first element in map
            pageable = PageRequest.of(page, pagesize,
                        Sort.by(orders));
        }
        var canDBPage = (sortmap == null || sortmap.isEmpty()) && pagesize != Integer.MAX_VALUE;
        List<Element> elements = null;
        elements = elementRepository.findElementsByArgs(type, ownerRepository.findByUsername(owner),
                searchargs, childargs, pageable, latestRevision, changedSince, canDBPage);
        if (elements == null) {
            logger.warn("Elements is null");
            return new ArrayList<Map<String, Object>>(0);
        }

        logger.debug("findElementsByArgs returned {} elements", elements.size());
        List<Map<String, Object>> r = new ArrayList<Map<String, Object>>(elements.size());
        logger.debug("findByType: BEGIN toMap");
        for (Element e : elements) {
            logger.debug("add element {} to r", e);
            r.add(e.toMap(pnames));
        }
        logger.debug("findByType: END toMap");

        //sort 0=DESC, 1=ASC
        if (sortmap != null && !sortmap.isEmpty()) {
            MapComparator cmp = new MapComparator();
            cmp.ignorecase = ignorecase;
            for (String key : sortmap.keySet()) {
                cmp.key = key;
                cmp.order = sortmap.get(key);
                Collections.sort(r, cmp);
            }
            logger.debug("findByType: sorted");

        }

        if (!canDBPage && pagesize != Integer.MAX_VALUE) {
            if (page * pagesize > r.size() - 1) return new ArrayList<Map<String, Object>>();
            return r.subList(page * pagesize, Math.min((page + 1) * pagesize, r.size()));
        }
        return r;
    }

    @Override
    public Map<String, Object> getModifiedProperties(
            Map<String, Object> elementMap) {
        logger.debug("element map {}", elementMap.keySet().size());
        if (elementMap.get("id") != null) {
            Long id = ((Number) elementMap.get("id")).longValue();
            final Map<String, Object> orig = getElementMap(id, false);
            if (orig != null) {
                final var ignoredProps = List.of("modcomment", "changed", "changer", "changername", "type", "ownername");
                var diff = Maps.diff(orig, elementMap);
                for (var p : ignoredProps) {
                    diff.remove((p));
                }
                return diff;
            }
        }
        return null; // new element
    }

    @Override
    public boolean checkVersion(final Long id, final Long version, String type) {
        /*
         * Type is only needed in Mongo -> ignore here
         */
        if (id == null || version == null)
            return false;
        Element e = elementRepository.findById(id).orElse(null);
        if (e == null)
            return false;
        return e.getVersion().equals(version);
    }

    @Override
    public Map<String, Object> getElementMap(Long id) {
        return getElementMap(id, true);
    }

    @Override
    public Map<String, Object> getElementMap(Long id, boolean withDbInfos) {
        Element e = elementRepository.findById(id).orElse(null);
        if (e != null){
            List<String> pnames = null;
            if (!withDbInfos){
                pnames = new ArrayList<>();
            }
            return e.toMap(pnames);
        }
        return null;
    }

    @Override
    public String getTypeOfElement(Long id) {
        Element e = elementRepository.findById(id).orElse(null);
        if (e != null) {
            logger.debug("Element with id {} is {}", id, e);
            return e.getElementType().getName();
        }
        logger.error("Element with id {} is NULL", id);
        return null;
    }

    public int changeGroupInElements(Group oldGroup, Group newGroup) {
        int changes = 0;

        List<Element> elements = elementRepository.findByGroup(oldGroup);
        logger.debug("Update elements: Found {}.", elements.size());

        for (Element element : elements) {
            logger.debug("Change Element {} from old group {} to new group {}", element.getId(), newGroup.getName(), oldGroup.getName());
            element.setGroup(newGroup);
            elementRepository.save(element);
            changes++;
        }

        logger.info("Remove group {}", oldGroup.getName());
        groupRepository.delete(oldGroup);

        return changes;
    }

    @Override
    public Map<String, Object> getElement(Long id, Long mod_id) {
        final Element e = elementRepository.findById(id).orElse(null);
        if (e != null)
            return e.toMap(mod_id);
        return null;
    }

    @Override
    public ElementType getElementType(String type) {
        return elementTypeRepositoryJpa.findByName(type);
    }

    @Override
    public Map<Long, Pair<Long, Long>> getOriginalIdsBeforeImport() {
        var mods = modificationRepositoryJpa.findByCommentLike(IMPORT_PREFIX + "%");
        var idMap = new HashMap<Long, Pair<Long, Long>>();

        for (var mod : mods) {
            var stringId = mod.getComment().replace(IMPORT_PREFIX, "").split(",");
            var pair = new ImmutablePair<>(
                Long.valueOf(stringId[0]),
                Long.valueOf(stringId[1])
            );
            idMap.put(mod.getElement().getId(), pair);
        }
        return idMap;
    }

    @Override
    public List<Map<String, Object>> getAllChangedSinceImport() {
        return modificationRepositoryJpa.findByCommentNotLike(IMPORT_PREFIX + "%").stream()
            .map(m -> m.getElement().toMap()).distinct().collect(Collectors.toList());
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
                Sort.by(sortDirection, "id"));

        List<TableModification> mods = modificationRepositoryJpa.getModificationsOfElement(elementId, pageable);
        return new ArrayList<Modification>(mods);
    }

    @Override
    public Map<String, Object> getElement(Long id, List<String> pnames) {
        final Element e = elementRepository.findById(id).orElse(null);
        if (e != null)
            return e.toMap(pnames);
        return null;
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteElement(Long id) {
        final Element e = elementRepository.findById(id).orElse(null);
        logger.debug("Delete Element with id={}: {}", id, e);
        if (e != null) {
            try {
                elementRepository.deleteById(id);
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
            for (ElementRefs r : e.getReferences()) {
                List<ElementRefList> el = r.getReflist();
                if (el == null || el.size() == 0)
                    continue;
                for (ElementRefList rl : el) {
                    for (Element e2 : rl.getElementList())
                        if (e2 != null)
                            deleteElement(e2.getId());
                }
            }
        }
    }

}
