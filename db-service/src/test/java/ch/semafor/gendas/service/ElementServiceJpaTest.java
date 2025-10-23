package ch.semafor.gendas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.semafor.gendas.dao.jpa.ElementRepositoryJpa;
import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Modification;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.Property;
import ch.semafor.gendas.model.PropertyType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("jpa")
@DataJpaTest
public class ElementServiceJpaTest {

	@TestConfiguration
	static class ElementServiceTestConfig {

		@Bean
		public ElementServiceJpa elementServiceJpa() {
			return new ElementServiceJpa();
		}
	}

	@MockBean
	private ElementRepositoryJpa elementRepositoryJpa;
	
	@Autowired
	private ElementServiceJpa elementServiceJpa;

	// Returned List of the createTestElements method
	private List<Element> elList;

	// ElementType which should be used
	private final String elementType = "ElementType";

	// Owner of the element
	private final Owner owner = null;

	// Map of search arguments
	private final Map<String, Object> searchargs = null;

	// Map of child arguments
	private final Map<String,Map<String,Object>> childargs = null;

	// Pageable object for search
	private final Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE,
			Sort.by(Sort.Direction.DESC, "id"));

	// Use latest revision
	private final boolean latest = true;

	// List of property names
	private final List<String> pnames = Arrays.asList("name", "rev", "owner", "group", "approval", "created", "changed");

	/**
	 * Set up mock of elementRepository for elementService test
	 * @throws CoreException
	 */
	@BeforeEach
	public void setUp() throws CoreException {
		elList = createTestElements(5,createType(elementType), pnames);

		Mockito.when(elementRepositoryJpa.findElementsByArgs(
				elementType, owner, searchargs, childargs, pageable, latest, null, false)).thenReturn(elList);
	}

	/**
	 * Find elements with the elementService
	 */
	@Test
	public void findByType() {
	  assertNotNull(elementRepositoryJpa);
	  Map<String,Integer> sortmap = null;

	  List<Map<String, Object>> expected = new ArrayList<>();

	  for( Element e : elList ){
	    expected.add(e.toMap(pnames));
	  }
	  String ownername = null;

	  List<Map<String, Object>> actual = elementServiceJpa.findByType(elementType, ownername, pnames,
			  searchargs, childargs, 0, Integer.MAX_VALUE, sortmap, latest);

	  assertEquals(expected, actual);
	}

	/**
	 * Create a new elementType
	 * @param type Name of the elementType
	 * @return Created elementType
	 */
	private ElementType createType( String type ){
		ElementType elType = new ElementType(type);
		elType.setId(1L);
		elType.setBeanId("id");
		elType.setBeanVersionId("version");
		return elType;
	}

	/**
	 * Creates a list of elements for test purposes
	 * @param size Size of returned arrayList
	 * @param type ElementType which should be used
	 * @param pnames List of propertyType names
	 * @return List of created elements
	 * @throws CoreException
	 */
	private List<Element> createTestElements(int size, final ElementType type, List<String> pnames)
			throws CoreException{

		Map<String, PropertyType> ptm = new HashMap<>();
		for( String name : pnames){
			ptm.put(name, new PropertyType(name, PropertyType.Type.STRING));
		}
		List<Element> elist = new ArrayList<>();
		for( int s=0; s<size;s++ ){
			Element elmnt = new Element(type);
			for( String typename : ptm.keySet()){
				Property p = new Property(elmnt,ptm.get(typename));
				p.setValue(0, typename);
			}
			elmnt.setId((long) s);
			elist.add(elmnt);
		}
		return elist;
	}

	/**
	 * Find the first modification of element 1 with the elementService
	 */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void getFirstModification_element1() {
    	Modification mod = elementServiceJpa.getFirstModification(1L);
    	assertEquals(1L, mod.getId(), "id");
    	assertEquals("created", mod.getComment());
    }

	/**
	 * Find the first modification of element 2 with the elementService
	 */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void getFirstModification_element2() {
    	Modification mod = elementServiceJpa.getFirstModification(2L);
    	assertEquals(2L, mod.getId());
    	assertEquals("created", mod.getComment());
    }

	/**
	 * Find the latest modification of element 1 with the elementService
	 */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void getLatestModification_element1() {
    	Modification mod = elementServiceJpa.getLatestModification(1L);
    	assertEquals(9L, mod.getId());
    	assertEquals("remove person", mod.getComment());
    }

	/**
	 * Find the latest modification of element 1 with the elementService
	 */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void getLatestModification_element2() {
    	Modification mod = elementServiceJpa.getLatestModification(2L);
    	assertEquals(2L, mod.getId());
    	assertEquals("created", mod.getComment());
    }

	/**
	 * Find latest modifications of element 1 with the elementService
	 */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void getModifications_latest_element1() {
    	List<Modification> mods = elementServiceJpa.getModifications(1L, 0, 5, Sort.Direction.DESC);
    	assertEquals(2, mods.size());
    	assertEquals(9L, mods.get(0).getId());
    	assertEquals(1L, mods.get(1).getId());
    	assertEquals("remove person", mods.get(0).getComment());
    	assertEquals("created", mods.get(1).getComment());
    }

	/**
	 * Find latest modifications of element 2 with the elementService
	 */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void getModifications_latest_element2() {
    	List<Modification> mods = elementServiceJpa.getModifications(2L, 0, 5, Sort.Direction.DESC);
    	assertEquals(1, mods.size());
    	assertEquals(2L, mods.get(0).getId());
    	assertEquals("created", mods.get(0).getComment());
    }

	/**
	 * Find first modifications of element 1 with the elementService
	 */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void getModifications_first_element1() {
    	// first modifications
    	List<Modification> mods = elementServiceJpa.getModifications(1L, 0, 5, Sort.Direction.ASC);
    	assertEquals(2, mods.size());
    	assertEquals(1L, mods.get(0).getId());
    	assertEquals(9L, mods.get(1).getId());
    	assertEquals( "created", mods.get(0).getComment());
    	assertEquals("remove person", mods.get(1).getComment());
    }

	/**
	 * Find first modifications of element 2 with the elementService
	 */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void getModifications_first_element2() {
    	List<Modification> mods = elementServiceJpa.getModifications(2L, 0, 5, Sort.Direction.ASC);
    	assertEquals(1, mods.size());
    	assertEquals(2L, mods.get(0).getId());
    	assertEquals( "created", mods.get(0).getComment());
    }
}
