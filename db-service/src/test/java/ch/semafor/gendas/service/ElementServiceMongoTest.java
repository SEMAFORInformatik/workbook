package ch.semafor.gendas.service;


import ch.semafor.gendas.dao.mongo.ElementTypeRepositoryMongo;
import ch.semafor.gendas.dao.mongo.ModificationRepositoryMongo;
import ch.semafor.gendas.events.ElementTypeModelListener;
import ch.semafor.gendas.events.ModificationModelListener;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.MapModification;
import ch.semafor.gendas.model.Modification;
import ch.semafor.gendas.model.Owner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("mongo")
@Testcontainers
@DataMongoTest//(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class ElementServiceMongoTest {
  @Container
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.2");
  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    mongoDBContainer.start();
    registry.add("spring.mongodb.host", mongoDBContainer::getHost);
    registry.add("spring.mongodb.port", mongoDBContainer::getFirstMappedPort);
  }

    private static final Logger logger= LoggerFactory.getLogger(ElementServiceMongoTest.class);

    private static final List<String> changeStrings = Arrays.asList(
        "Created something",
        "First modification",
        "Second modification");
    private static final Long elementRef1 = Long.parseLong("456");
    private static final Long elementRef2 = Long.parseLong("457");

    @TestConfiguration
    static class ElementServiceMongoTestConfig {

        @Bean
        public ElementTypeModelListener elementTypeModelListener() {
            return new ElementTypeModelListener();
        }

        @Bean
        public ModificationModelListener modificationModelListener() {
            return new ModificationModelListener();
        }

        @Bean
        public SequenceGeneratorService sequenceGeneratorService() {
            return new SequenceGeneratorService();
        }

        @Bean
        public ElementServiceMongo elementServiceMongo() {
            return new ElementServiceMongo();
        }
    }

    @Autowired
    private ElementServiceMongo elementServiceMongo;

    @Autowired
    private ElementTypeRepositoryMongo elementTypeRepositoryMongo;

    @Autowired
    ModificationRepositoryMongo modificationRepositoryMongo;

    @BeforeEach
    public void setUp() {
        elementTypeRepositoryMongo.deleteAll();
        modificationRepositoryMongo.deleteAll();

        Owner owner = new Owner("hans");
        Map<String, Object> diff = new HashMap<>();

        for (String changeString : changeStrings) {
            MapModification mapMod = new MapModification(elementRef1, owner, changeString, diff);
            modificationRepositoryMongo.save(mapMod);
            if (changeString != "Second modification") {
            	mapMod = new MapModification(elementRef2, owner, changeString, diff);
            	modificationRepositoryMongo.save(mapMod);
            }
        }
    }

    /**
     * Create and save a new ElementType
     */
    @Test
    public void createElementType() {
        String name = "Recipe";
        List<Map<String, Object>> props = createTypeList("recipe.json");
        ElementType et = elementServiceMongo.createElementType(name, props, "id", "version");
    }

    /**
     * Read recipe definition from json file and create a list
     * @return list of recipe properties
     */
    private static List<Map<String, Object>> createTypeList(String filename) {
        List<Map<String, Object>> elementType = new ArrayList<>();
        Resource resource = new ClassPathResource(filename);
        ObjectMapper mapper = new ObjectMapper();
        try {
            elementType = mapper.readValue(resource.getInputStream(), List.class);
        } catch (IOException e) {
            logger.warn("Could not read from file {} in classpath", filename);
            e.printStackTrace();
        }
        logger.info("Generated List: {}", elementType.toString());
        return elementType;
    }

	/**
	 * Find the first modification of element 1 with the elementService
	 */
    @Test
    public void getFirstModification_element1() {
    	Modification mod = elementServiceMongo.getFirstModification(elementRef1);
    	assertEquals(changeStrings.get(0), mod.getComment());
    }

	/**
	 * Find the first modification of element 2 with the elementService
	 */
    @Test
    public void getFirstModification_element2() {
    	Modification mod = elementServiceMongo.getFirstModification(elementRef2);
    	assertEquals(changeStrings.get(0), mod.getComment());
    }

	/**
	 * Find the latest modification of element 1 with the elementService
	 */
    @Test
    public void getLatestModification_element1() {
    	Modification mod = elementServiceMongo.getLatestModification(elementRef1);
    	assertEquals(changeStrings.get(2), mod.getComment());
    }

	/**
	 * Find the latest modification of element 1 with the elementService
	 */
    @Test
    public void getLatestModification_element2() {
    	Modification mod = elementServiceMongo.getLatestModification(elementRef2);
    	assertEquals(changeStrings.get(1), mod.getComment());
    }

	/**
	 * Find latest modifications of element 1 with the elementService
	 */
    @Test
    public void getModifications_latest_element1() {
    	List<Modification> mods = elementServiceMongo.getModifications(elementRef1, 0, 5, Sort.Direction.DESC);
    	assertEquals(3, mods.size());
    	assertEquals(changeStrings.get(2), mods.get(0).getComment());
    	assertEquals( changeStrings.get(1), mods.get(1).getComment());
    	assertEquals(changeStrings.get(0), mods.get(2).getComment());
    }

	/**
	 * Find latest modifications of element 2 with the elementService
	 */
    @Test
    public void getModifications_latest_element2() {
    	List<Modification> mods = elementServiceMongo.getModifications(elementRef2, 0, 5, Sort.Direction.DESC);
    	assertEquals(2, mods.size());
    	assertEquals(changeStrings.get(1), mods.get(0).getComment());
    	assertEquals(changeStrings.get(0), mods.get(1).getComment());
    }

	/**
	 * Find first modifications of element 1 with the elementService
	 */
    @Test
    public void getModifications_first_element1() {
    	// first modifications
    	List<Modification> mods = elementServiceMongo.getModifications(elementRef1, 0, 5, Sort.Direction.ASC);
    	assertEquals(3, mods.size());
    	assertEquals(changeStrings.get(0), mods.get(0).getComment());
    	assertEquals(changeStrings.get(1), mods.get(1).getComment());
    	assertEquals(changeStrings.get(2), mods.get(2).getComment());
    }

	/**
	 * Find first modifications of element 2 with the elementService
	 */
    @Test
    public void getModifications_first_element2() {
    	List<Modification> mods = elementServiceMongo.getModifications(elementRef2, 0, 5, Sort.Direction.ASC);
    	assertEquals( 2, mods.size());
    	assertEquals(changeStrings.get(0), mods.get(0).getComment());
    	assertEquals(changeStrings.get(1), mods.get(1).getComment());
    }
}
