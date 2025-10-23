package ch.semafor.gendas.service;

import ch.semafor.gendas.dao.jpa.ElementTypeRepositoryJpa;
import ch.semafor.gendas.dao.jpa.PropertyTypeRepositoryJpa;
import ch.semafor.gendas.model.ElementType;
import com.mycompany.customerrelations.Person;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("jpa")
@Transactional
@DataJpaTest
public class ElementTypeCreatorTest {
	private static final Logger logger = LoggerFactory.getLogger(ElementTypeCreatorTest.class);

	@Resource
	private ElementTypeRepositoryJpa elementTypeRepositoryJpa;

	@Resource
	private PropertyTypeRepositoryJpa propertyTypeRepositoryJpa;

	@Test
	public void createFromMap() {
	    final ElementTypeCreator creator =
	    		new ElementTypeCreator(elementTypeRepositoryJpa, propertyTypeRepositoryJpa);

	    String id="id";
		String version="version";
		List<Map<String, Object>> typedef = new ArrayList<Map<String, Object>>();
		
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", "p1");
		m.put("type", "STRING");
		typedef.add(m);
		m = new HashMap<String, Object>();
		m.put("name", "p2");
		m.put("type", "INTEGER");
		typedef.add(m);

		List<Map<String,Object>> props2 = new ArrayList<Map<String, Object>>();
 		m = new HashMap<String, Object>();
		m.put("name", "p1");
		m.put("type", "STRING");
		props2.add(m);
		m = new HashMap<String, Object>();
		m.put("name", "p3");
		m.put("type", "REAL");
		props2.add(m);

		m = new HashMap<String, Object>();
		m.put("name", "mysubtyperef");
		m.put("type", "subtyperef");
		m.put("props", props2);
		
		typedef.add( m );
		
		ElementType t = creator.create("mytype", typedef, id, version);
		assertEquals("mytype", t.getName());
		assertEquals(id, t.getBeanId());
		assertEquals(version, t.getBeanVersionId());
		assertEquals("subtyperef", t.getElementType("mysubtyperef").getName());
		assertEquals(2, t.getPropertyTypes().size());
		// check if create is idempotent (no unique index or primary key exceptions)
		m = new HashMap<String, Object>();
		m.put("name", "created");
		m.put("type", "DATE");
		typedef.add(m);

		ElementType t2 = creator.create("mytype", typedef, id, version);
		assertEquals("java.lang.String", t2.getPropertyType("p1").getType().getName());
		// TODO: fix this getPropertyType("created") is null
		// assertEquals("java.util.Date", t2.getPropertyType("created").getType().getName());
	}

	@Test
	@Sql({"/gendas-data.sql"})
	public void update(){
		final ElementTypeCreator creator = new ElementTypeCreator(elementTypeRepositoryJpa, propertyTypeRepositoryJpa);
		ElementType t = creator.create(Person.class, null, null);

		assertEquals(10, t.getPropertyTypes().size());
		creator.reset();
		List<Map<String, Object>> typedef = new ArrayList<Map<String, Object>>();
		Map<String, Object> p1def = new HashMap<String, Object>();
		p1def.put("name","p1");
		p1def.put("type", "STRING");
		typedef.add(p1def);
		t = creator.create(t.getName(), typedef, null, null);		
		assertEquals(11, t.getPropertyTypes().size());
	}
	
	@Test
	public void createFromClass() {
	    final ElementTypeCreator creator = new ElementTypeCreator(elementTypeRepositoryJpa, propertyTypeRepositoryJpa);

	    String id="id";
		String version="version";
	    
		ElementType t = creator.create(Person.class, id, version);
		assertEquals("com.mycompany.customerrelations.Person", t.getName());
//		for( PropertyType p: t.getPropertyTypes()){
//			System.out.println(p.toString());
//		}
		assertEquals(6, t.getPropertyTypes().size());
		assertEquals("com.mycompany.customerrelations.MaritalStatusCode", t.getElementType("maritalStatus").getName());
	    
	}
}
