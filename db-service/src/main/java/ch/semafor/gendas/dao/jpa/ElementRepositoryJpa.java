package ch.semafor.gendas.dao.jpa;

import ch.semafor.gendas.dao.jpa.custom.ElementRepositoryJpaCustom;
import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.model.Group;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Profile("jpa")
public interface ElementRepositoryJpa extends JpaRepository<Element, Long>, ElementRepositoryJpaCustom {

    /**
     * find elements of group
     *
     * @param group name (PK)
     * @return list of found elements in group
     */
    List<Element> findByGroup(final Group group);

    /**
     * Find Elements by their ElementType
     *
     * @param type ElementyType of the Element
     * @return List of Element obj
     */
    @Query("SELECT e FROM Element e WHERE e.elementType.name=?1 "
            + "AND 9999999 = (SELECT MAX(m.nextRevision) FROM e.modifications m)")
    List<Element> findByType(final String type);
}
