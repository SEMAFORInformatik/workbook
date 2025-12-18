package ch.semafor.gendas.dao.mongo;

import ch.semafor.gendas.events.MapModelListener;
import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.model.*;
import ch.semafor.gendas.service.SequenceGeneratorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("mongo")
@Testcontainers
@DataMongoTest()//excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class ElementTest extends RepositoryMongoTest {
    private static final Logger logger = LoggerFactory.getLogger(ElementTest.class);
    @TestConfiguration
    static class ElementRepositoryMongoTestConfig {

        @Bean
        public SequenceGeneratorService sequenceGeneratorService() {
            return new SequenceGeneratorService();
        }

        @Bean
        public MapModelListener beforeMapSaveListener() {
            return new MapModelListener();
        }
    }
    @Autowired
    ElementRepositoryMongo elementRepositoryMongo;

    @Autowired
    OwnerRepositoryMongo ownerRepositoryMongo;

    @Test
    public void createElement() throws CoreException {
        String type1 = "Customer";
        String type2 = "Address";

        ElementType et1 = new ElementType(type1);
        ElementType et2 = new ElementType(type2);

        Group group = new Group("Semafor");

        Owner owner = new Owner("hans");
        owner.setFirstName("Hans");
        owner.setLastName("Gerber");
        owner.setEnabled(true);
        owner.setPassword("hans_pw");
        owner.setActiveGroup(group);

        ownerRepositoryMongo.save(owner);

        Element cust1 = new Element(et1);
        Element addr = new Element(et2);
        addr.setOwner(owner);
        cust1.setOwner(owner);
        cust1.addElement("address", addr);

        Property city = new Property(addr, new PropertyType("city"));
        city.setValue(0, "basel");

        Property age = new Property(cust1, new PropertyType("age"));
        age.setValue(0, "34");

        Map<String, Object> map = elementRepositoryMongo.save(type1, cust1.toMap());
        map.remove("id");

        assertEquals(cust1.toMap(), map);
    }

    @AfterEach
    public void tearDown() throws Exception {
        elementRepositoryMongo.deleteAll();
    }
}
