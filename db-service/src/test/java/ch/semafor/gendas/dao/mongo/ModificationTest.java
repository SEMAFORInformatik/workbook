package ch.semafor.gendas.dao.mongo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.semafor.gendas.events.ModificationModelListener;
import ch.semafor.gendas.model.MapModification;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.service.SequenceGeneratorService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("mongo")
@Testcontainers
@DataMongoTest//(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class ModificationTest extends RepositoryMongoTest {
    private static final List<String> changeStrings = Arrays.asList("Created something",
        "First modification",
        "Second modification");
    private static final Long elementRef = Long.parseLong("456");

    @TestConfiguration
    static class ModificationRepositoryMongoTestConfig {

        @Bean
        public ModificationModelListener modificationModelListener() {
            return new ModificationModelListener();
        }

        @Bean
        public SequenceGeneratorService sequenceGeneratorService() {
            return new SequenceGeneratorService();
        }
    }

    @Autowired
    ModificationRepositoryMongo modificationRepositoryMongo;

    @BeforeEach
    public void setup() {
        Owner owner = new Owner("hans");
        Map<String, Object> diff = new HashMap<>();

        for (String changeString : changeStrings) {
            MapModification mapMod = new MapModification(elementRef, owner, changeString, diff);
            modificationRepositoryMongo.save(mapMod);
        }
    }

    @Test
    public void findByElementRef_first() {
        List<MapModification> mods = modificationRepositoryMongo.findAllOfElement(elementRef,
        		PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "_id")));
        assertEquals(1, mods.size());
        assertEquals( changeStrings.get(0), mods.get(0).getComment());
    }

    @Test
    public void findByElementRef_last() {
    	List<MapModification> mods = modificationRepositoryMongo.findAllOfElement(elementRef,
    			PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "_id")));
        assertEquals(1, mods.size());
    	assertEquals(changeStrings.get(2), mods.get(0).getComment());
    }

    @Test
    public void findByElementRef_all_descending() {
    	List<MapModification> mods = modificationRepositoryMongo.findAllOfElement(elementRef,
    			PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "_id")));
        assertEquals(3, mods.size());
        assertEquals( changeStrings.get(2), mods.get(0).getComment());
        assertEquals( changeStrings.get(1), mods.get(1).getComment());
        assertEquals( changeStrings.get(0), mods.get(2).getComment());
    }

    @Test
    public void findByElementRef_all_ascending() {
        List<MapModification> mods = modificationRepositoryMongo.findAllOfElement(elementRef,
        		PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "_id")));
        assertEquals(3, mods.size());
        assertEquals( changeStrings.get(0), mods.get(0).getComment());
        assertEquals(changeStrings.get(1), mods.get(1).getComment());
        assertEquals( changeStrings.get(2), mods.get(2).getComment());
    }

    @Test
    public void findByElementRef_all_default() {
    	List<MapModification> mods = modificationRepositoryMongo.findAllOfElement(elementRef, null);
        assertEquals(3, mods.size());
        assertEquals(changeStrings.get(0), mods.get(0).getComment());
        assertEquals( changeStrings.get(1), mods.get(1).getComment());
        assertEquals( changeStrings.get(2), mods.get(2).getComment());
    }

    @AfterEach
    public void tearDown() {
       modificationRepositoryMongo.deleteAll();
    }

}
