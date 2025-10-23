package ch.semafor.gendas.service;


import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Modification;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ElementService {
    public static final String IMPORT_PREFIX = "{import}:";
    /**
     * creates a new Element Type with corresponding Property Types
     *
     * @param typename  name of type
     * @param typedef   map with prop type definitions
     * @param idName    name of key value
     * @param idVersion name of version field for optimistic locking
     * @return created element type
     */
    ElementType createElementType(String typename, List<Map<String,
            Object>> typedef, String idName, String idVersion);

    /**
     * insert or update bean from map
     *
     * @param map           representing bean
     *                      to be persisted
     * @param type          name of Type
     *                      to be persisted
     * @param username      username of owner
     * @param changeComment
     * @return persisted element
     * @throws CoreException
     * @throws UsernameNotFoundException
     */
    Map<String, Object> save(final Map<String, Object> map, String type, String username, String changeComment)
            throws UsernameNotFoundException, CoreException;

    /**
     * get list of maps by type
     *
     * @param type       name of element type
     * @param owner      name of element owner
     * @param searchargs map of search conditions
     * @param pnames     list of property names to be extracted
     * @param page       number of page if paging mode
     * @param pagesize   size of page (paging mode if &gt; 0)
     * @param sortmap    map of properties for sort order (1: DESC, -1: ASC)
     * @param changedSince last modification within this date
     * @return map data
     */
    List<Map<String, Object>> findByType(final String type, final String owner,
                                         final List<String> pnames,
                                         final Map<String, Object> searchargs,
                                         final Map<String, Map<String, Object>> childargs,
                                         int page, int pagesize,
                                         Map<String, Integer> sortmap,
                                         boolean latestRevision, Date changedSince);

    List<Map<String, Object>> findByType(final String type, final String owner,
                                         final List<String> pnames,
                                         final Map<String, Object> searchargs,
                                         final Map<String, Map<String, Object>> childargs,
                                         int page, int pagesize,
                                         Map<String, Integer> sortmap,
                                         boolean latestRevision);

    /**
     * perform version check (optimistic locking)
     *
     * @param id      of element
     * @param version of element
     * @return true if modifiable
     */
    boolean checkVersion(Long id, Long version, String type);

    /**
     * get modified properties
     *
     * @param elementMap with properties of new element
     * @return map of modified properties (empty if not modified or null if new)
     * @throws UsernameNotFoundException
     */
    Map<String, Object> getModifiedProperties(
            Map<String, Object> elementMap);

    /**
     * get element Map by id
     *
     * @param id
     * @return map
     */
    Map<String, Object> getElementMap(Long id);

    /**
     * get element Map by id
     *
     * @param id
     * @param withDbInfos: true: include type, modcomment, changer, changername, changed
     * @return map
     */
    Map<String, Object> getElementMap(Long id, boolean withDbInfos);

    /**
     * get type of element by id
     *
     * @param id
     * @return type
     */
    String getTypeOfElement(Long id);

    /**
     * Rename speficic group in all elements
     *
     * @param oldGroup
     * @param newGroup
     * @return number of changes
     */
    int changeGroupInElements(Group oldGroup, Group newGroup);

    /**
     * get element by id
     *
     * @param id
     * @param pnames list of property names to be extracted
     * @return map
     */
    Map<String, Object> getElement(Long id, final List<String> pnames);

    /**
     * get element by id and mod_id
     *
     * @param id
     * @param mod_id
     * @return map
     * @throws CoreException
     */
    Map<String, Object> getElement(Long id, Long mod_id) throws CoreException;

    ElementType getElementType(String type);

    Map<Long, Pair<Long, Long>> getOriginalIdsBeforeImport();

    List<Map<String, Object>> getAllChangedSinceImport();

    Modification getFirstModification(Long id);

    Modification getLatestModification(Long id);

    List<Modification> getModifications(Long id, int page, int pagesize, Sort.Direction sortDirection);

    List<ElementType> getAllElementTypes();

    List<Group> getAllGroups();

    /**
     * delete element by id
     *
     * @param id
     * @throws CoreException
     */
    void deleteElement(Long id) throws CoreException;

}
