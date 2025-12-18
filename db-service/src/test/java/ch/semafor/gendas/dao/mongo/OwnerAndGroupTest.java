package ch.semafor.gendas.dao.mongo;

import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("mongo")
@Testcontainers
@DataMongoTest//(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class OwnerAndGroupTest extends RepositoryMongoTest {

    private static final Logger logger = LoggerFactory.getLogger(OwnerAndGroupTest.class);

    @Autowired
    private OwnerRepositoryMongo ownerRepositoryMongo;

    Owner owner1 = new Owner("bob");
    Owner owner2 = new Owner("hans");

    Group group1 = new Group("SEMAFOR");
    Group group2 = new Group("VIP");
    Group group3 = new Group("TESTING");
    Role role1 = new Role("ADMIN");

    @BeforeEach
    public void setUp() {
        Set<Role> roleset = new HashSet<>();
        roleset.add(role1);

        Set<Group> groupset1 = new HashSet<>();
        groupset1.add(group1);
        groupset1.add(group2);

        Set<Group> groupset2 = new HashSet<>();
        groupset2.add(group1);
        groupset1.add(group3);

        owner1.setGroups(groupset1);
        owner1.setRoles(roleset);
        owner1.setActiveGroup(group2);

        owner2.setGroups(groupset2);
        owner2.setActiveGroup(group1);
        // Save owners
        ownerRepositoryMongo.save(owner1);
        ownerRepositoryMongo.save(owner2);
    }

    @Test
    public void findAllOwners() {
        // find owners ordered by their username
        List<Owner> owners = ownerRepositoryMongo.findAllByOrderByUsernameAsc();

        // check if owners are found in the right order
        assertEquals(2, owners.size());
        assertEquals(owner1.getUsername(), owners.get(0).getUsername());
    }

    @Test
    public void findOwnerByUsername() {
        Owner owner = ownerRepositoryMongo.findByUsername("bob");
        assertNotNull(owner.getUsername());
    }

    @Test
    public void findOwnerByUsernameLike() {
        assertEquals(1, ownerRepositoryMongo.findByUsernameLike("bo").size());
    }

    @Test
    public void saveOwnerAndGroup() {
        owner1.addGroup(group1);
        owner1.setActiveGroup(group1);
        Owner owner = ownerRepositoryMongo.save(owner1);

        assertEquals(owner1.getGroups().size(), owner.getGroups().size());
        assertEquals(owner1.getActiveGroup(), owner.getActiveGroup());
    }

    @Test
    public void saveOwnerAndRole() {
        owner2.addRole(role1);
        Owner owner = ownerRepositoryMongo.save(owner2);

        assertEquals(owner2.getRoles().size(), owner.getRoles().size());
        assertEquals(owner2.getRoles().toArray()[0], owner.getRoles().toArray()[0]);
    }

    @Disabled
    public void findAllGroups(@Autowired GroupRepositoryMongo groupRepositoryMongo) {
        List<Group> groups = groupRepositoryMongo.findAll();
        logger.info("Found {} groups: {}", groups.size(), groups);
        assertEquals(groups.size(), 3);
    }

    @AfterEach
    public void tearDown() {
        ownerRepositoryMongo.deleteAll();
    }

}
