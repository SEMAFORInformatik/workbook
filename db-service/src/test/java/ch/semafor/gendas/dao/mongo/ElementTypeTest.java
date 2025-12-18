package ch.semafor.gendas.dao.mongo;

import ch.semafor.gendas.events.ElementTypeModelListener;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.service.SequenceGeneratorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("mongo")
@Testcontainers
@DataMongoTest//(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class ElementTypeTest extends RepositoryMongoTest {

    @TestConfiguration
    static class ElementTypeRepositoryMongoTestConfig {

        @Bean
        public ElementTypeModelListener elementTypeModelListener() {
            return new ElementTypeModelListener();
        }

        @Bean
        public SequenceGeneratorService sequenceGeneratorService() {
            return new SequenceGeneratorService();
        }
    }

    @Autowired
    private ElementTypeRepositoryMongo elementTypeRepository;

    @Test
    public void findbByElementTypeNotExisting(){
        // check if no type will be returned if name is not existing
        assertNull(elementTypeRepository.findByName("notexisting"));
    }

    @Test
    public void findbByElementTypeandName() {
        elementTypeRepository.save(new ElementType("com.mycompany.customerrelations.Customer"));
        // find an elementtype by his name
        String type = "com.mycompany.customerrelations.Customer";
        ElementType et = elementTypeRepository.findByName(type);

        // check if name is equal to created string
        assertEquals(type, et.getName());
    }

    @Test
    public void saveElementType(){
        // create a new elementtype
        ElementType et = new ElementType("com.mycompany.customerrelations.Group");

        // save elementtype into the db
        ElementType db_et = elementTypeRepository.save(et);

        // check if elementtype is the same and is existing
        assertNotNull(db_et.getId());
        assertEquals(et.getName(), db_et.getName());
    }

    @AfterEach
    public void tearDown() {
        elementTypeRepository.deleteAll();
    }

}
