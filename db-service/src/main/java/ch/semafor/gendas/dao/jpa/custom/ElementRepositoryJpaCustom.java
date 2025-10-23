package ch.semafor.gendas.dao.jpa.custom;

import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import org.hibernate.stat.Statistics;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ElementRepositoryJpaCustom {

    /**
     * get cache statistics
     *
     * @return statistics
     */
    Statistics getStatistics();

    /**
     * get all active (not deleted) elements
     *
     * @return list of active elements
     */
    List<Element> getAllActive();

    /**
     * find elements of group
     *
     * @param group name (PK)
     * @return list of found elements in group
     */
    List<Element> findByGroup(final Group group);

    /**
     * find elements of given type and properties
     *
     * @param etype          name of element type
     * @param owner          name of owner
     * @param args           map of key-value pairs
     * @param childargs      map of child components with key-value pairs
     * @param pageable       Pageable Object
     * @param latestRevision if true search latest revisions only otherwise search all
     * @return list of found elements
     */
    List<Element> findElementsByArgs(final String etype, final Owner owner, final Map<String, Object> args,
                                     final Map<String, Map<String, Object>> childargs, Pageable pageable,
                                     boolean latestRevision, Date changedSince, boolean canDBPage);
}
