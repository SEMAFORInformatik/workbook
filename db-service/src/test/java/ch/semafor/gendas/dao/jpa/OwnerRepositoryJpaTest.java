package ch.semafor.gendas.dao.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("jpa")
@DataJpaTest
public class OwnerRepositoryJpaTest {

    @Autowired
    private OwnerRepositoryJpa ownerRepositoryJpa;

    @Test
    @Sql("/gendas-data.sql")
    public void findAllOwners() {
        // find owners ordered by their username
        List<Owner> owners = ownerRepositoryJpa.findAllByOrderByUsernameAsc();

        // check if owners are found in the right order
        assertEquals(3, owners.size());
        assertEquals("bob", owners.get(1).getUsername());
    }

    @Test@Sql("/gendas-data.sql")
    public void findOwnerLike() {
        // find owner bob with string %b%
        assertEquals(1, ownerRepositoryJpa.findByUsernameLike("%bo%").size());
    }

    @Test@Sql("/gendas-data.sql")
    public void findOwnerByUsername() {
        // find owner with different case
        Owner fred = ownerRepositoryJpa.findByUsername("fred");

        // check if owner an roles are found
        assertEquals("fred", fred.getUsername());
        assertEquals(2, fred.getRoles().size());
    }

    @Test
    public void saveOwnerAndRole() {
        Owner dave = new Owner("dave");
        dave.setPassword("davespassword");
        dave.addRole(new Role("ADMIN"));

        Owner dbdave = ownerRepositoryJpa.save(dave);
        assertEquals(dave.getUsername(), dbdave.getUsername());
        assertEquals(dave.getRoles().size(), dbdave.getRoles().size());
    }

    @Test
    public void saveOwnerAndGroup() {
        Owner max = new Owner("max");
        max.setPassword("maxspasswords");
        max.addGroup(new Group("groupB"));
        max.setActiveGroup(new Group("groupA"));

        Owner dbmax = ownerRepositoryJpa.save(max);
        assertEquals(max.getGroups().size(), dbmax.getGroups().size());
    }

}
