package ch.semafor.gendas.dao.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.semafor.gendas.model.Group;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("jpa")
@DataJpaTest
public class GroupRepositoryJpaTest {

    @Autowired
    private GroupRepositoryJpa groupRepositoryJpa;

    @Test
    @Sql("/gendas-data.sql")
    public void findGroupByName() {
        // find one of the groups in the db
        Group dbgroup = groupRepositoryJpa.findByName("groupA");

        // check if it is the right group
        assertEquals(dbgroup, new Group("groupA"));
    }

    @Test@Sql("/gendas-data.sql")
    public void findAllGroups() {
        // find all groups in the db
        List<Group> groupList = groupRepositoryJpa.findAll();

        // check if all groups are found
        assertEquals(2, groupList.size());
    }

    @Test
    public void saveGroup() {
        // create a new group
        Group group = new Group("groupC");

        // save the group with the repository
        Group dbgroup = groupRepositoryJpa.save(group);

        // check if the two groups are equal
        assertEquals(dbgroup.toString(), group.toString());
    }

    @Test
    public void saveAndDeleteGroup() {
        // create a new group
        Group group = new Group("groupD");

        // save the group with the repository
        group = groupRepositoryJpa.save(group);

        // check if group is stored
        assertEquals(1, groupRepositoryJpa.count());

        // delete the given group
        groupRepositoryJpa.delete(group);

        // check if there are no more groups
        assertEquals(0, groupRepositoryJpa.count());
    }

}
