package ch.semafor.gendas.dao;

import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.model.Group;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface ElementRepository {


    /**
     * find elements of group
     *
     * @param group name (PK)
     * @return list of found elements in group
     */
    List<Element> findByGroup(final Group group);

    /**
     * get list of elements by type
     *
     * @param type
     * @return list of elements
     */
    List<Element> findByType(final String type);

}
