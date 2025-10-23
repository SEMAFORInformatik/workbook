package ch.semafor.gendas.dao;

import ch.semafor.gendas.model.Group;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface GroupRepository extends CrudRepository<Group, String> {

    /**
     * return a group by name
     *
     * @param name (string) to identify a group
     * @return the persisted group object by name
     */
    Group findByName(String name);

    /**
     * get all groups from the database
     *
     * @return list of groups
     */
    List<Group> findAll();

}
