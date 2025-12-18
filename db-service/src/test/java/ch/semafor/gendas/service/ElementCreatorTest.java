/*
 * Copyright 2010 Semafor Informatik & Energie AG, Basel, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 */
package ch.semafor.gendas.service;

import ch.semafor.gendas.dao.jpa.*;
import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.ElementCreationException;
import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.TableModification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.customerrelations.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("jpa")
@Transactional
@DataJpaTest
public class ElementCreatorTest {
  private static final Logger logger = LoggerFactory.getLogger(ElementCreatorTest.class);
  @Resource
  private ElementRepositoryJpa elementRepository;
  @Resource
  private OwnerRepositoryJpa ownerRepository;
  @Resource
  private GroupRepositoryJpa groupRepository;
  @Resource
  private ElementTypeRepositoryJpa elementTypeRepositoryJpa;
  @Resource
  private PropertyTypeRepositoryJpa propertyTypeRepositoryJpa;

  private void save( Object bean, String comment ) throws CoreException, ElementCreationException {
    final ElementCreator creator = new ElementCreator(elementTypeRepositoryJpa,
    		elementRepository, ownerRepository, groupRepository, propertyTypeRepositoryJpa);
    final Element newElement = creator.create(bean,null);
    newElement.crunch();
    if( logger.isDebugEnabled()){
        newElement.print(0, "new Element after crunch()");
    }

    Element element = new Element( newElement.getElementType() );
    element.assign( newElement, new ArrayList<Element>(), elementRepository );
    if( logger.isDebugEnabled()){
        element.print(0, "Element after assign() before save()");
    }
    TableModification mod = element.getLastModification();
    mod.setComment(comment);
    element = elementRepository.save( element );
    assertNotNull(element.getId());
    creator.setId(bean, element.getId(), "id");
    creator.setMatchingIdsAndVersions(bean, element, null);
 //   creator.setVersion(bean,Integer.valueOf(element.getVersion().intValue()));
  }

  private Object load(final Long id) throws CoreException {
	final ElementCreator creator = new ElementCreator(elementTypeRepositoryJpa,
			elementRepository, ownerRepository, groupRepository, propertyTypeRepositoryJpa);
	final Element element = elementRepository.findById(id).orElse(null);
    return creator.load(element);
  }

    @Test@Sql({"/gendas-data.sql"})
  public void createElement() throws CoreException, ElementCreationException {
      final List<Element> genderListNew= elementRepository.findByType("com.mycompany.customerrelations.GenderCode");
      for(Element e : genderListNew) {
          logger.debug("Before: " + e.toString());
      }

    Person person = new Person();
    person.setFirstname("first");
    person.setText1("AAA");
    person.setText2("BBB");
    person.setText3("CCC");
    final GenderCode female = new GenderCode(); 
    female.setId(5L);
    female.setValue("female");
    final GenderCode male = new GenderCode();
    male.setId(6L);
    male.setValue("male");

    person.setGender(female);
    save(person, "creation test");
    assertNotNull(person.getId());
        
    person = (Person) load(person.getId());
    assertEquals("female", person.getGender().getValue());
    
    person.setGender(male);
    person.setText1("AAAAAA");
    person.setText2("BBBBBB");
    person.setText3("CCCCCC");
    save( person, "created" );
    final List<Element> genderList= elementRepository.findByType("com.mycompany.customerrelations.GenderCode");
    for(Element e : genderList) {
        logger.debug("After: " + e.toString());
    }

    assertEquals(2, genderList.size());

    person = (Person) load(person.getId());
    assertEquals("male", person.getGender().getValue());
  }
  
    @Test
    @Sql({"/gendas-data.sql"})
  public void createElementWithArrayRefs() throws CoreException, ElementCreationException {
    Customer cust = new Customer();
    cust.setPremium(true);
    cust.setAddresses(new ArrayList<Address>());
    Address addr = new Address();
    addr.setCity("city1");
    cust.getAddresses().add(addr);

    save( cust, "creation with array refs" );
    
    addr=new Address();
    addr.setCity("city2");
    cust.getAddresses().add(addr);

    save( cust, "creation with array refs" );
    
    cust= (Customer)load(cust.getId());

    assertEquals(2, cust.getAddresses().size());
      assertTrue(cust.isPremium());
  }
  
    @Test@Sql({"/gendas-data.sql"})
  public void createAndModifyElementWithRef() throws CoreException, ElementCreationException  {
	final GenderCode female = new GenderCode();
    female.setValue("female");
    save( female, "set gender code" );
    final GenderCode male = new GenderCode();
    male.setValue("male");
    save( male , "set gender code");
    
    final MaritalStatusCode married=new MaritalStatusCode();
    married.setValue("married");
    save(married, "set marital code");
    
    final Person person = new Person();
    person.setMaritalStatus(married);
    person.setGender(female);
    save( person, "created" );
    assertNotNull( person.getId() );

    Customer cust = new Customer();
    cust.setPerson(person);
    save(cust, "created");

    if( logger.isDebugEnabled() ){
        logger.debug("CustomerId: {}", cust.getId());
        cust = (Customer)load(cust.getId());
        logger.debug("Customer gender: {}", cust.getPerson().getGender().getValue());
        logger.debug("Customer maritalStatus: {}", cust.getPerson().getMaritalStatus().getValue());
    }
  }

    @Test@Sql({"/gendas-data.sql"})
  public void removeElementRefs() throws CoreException, ElementCreationException  {
    final Customer cust = (Customer) load(1L);
    List<Address> addresses = cust.getAddresses();
    assertEquals(1, addresses.size());
    addresses = new ArrayList<Address>();
    cust.setAddresses( addresses );
    System.out.println( cust.getCredits().get(0));
    save(cust, "added addresses");
    assertEquals(0, cust.getAddresses().size());
    }
  
    @Test@Sql({"/gendas-data.sql"})
  public void createElementFromMapWithMatrix() throws ElementCreationException, CoreException{
	  final Map<String,Object> data = new HashMap<String, Object>();
	  Integer nr = 111111;
	  data.put("customerNumber", nr);
	  data.put("credits", new ArrayList<List<Integer>>());
	  ((List)data.get("credits")).add(new ArrayList<Integer>());
	  ((List<Double>)((List)data.get("credits")).get(0)).add(new Double(11));
	  ((List<Double>)((List)data.get("credits")).get(0)).add(new Double(12));
	  ((List)data.get("credits")).add(new ArrayList<Integer>());
	  ((List<Double>)((List)data.get("credits")).get(1)).add(new Double(21));
	  ((List<Double>)((List)data.get("credits")).get(1)).add(new Double(22));
	  List<String>strlist=new ArrayList<String>();
	  strlist.add("firstname");
	  List plist = new ArrayList<Map<String,Object>>();
	  Map<String,Object> m = new HashMap<String, Object>();
	  m.put("firstname", strlist);
	  plist.add(m);
	  data.put("person", plist);
	  
	  final ElementCreator creator = new ElementCreator(elementTypeRepositoryJpa,
			  elementRepository, ownerRepository, groupRepository, propertyTypeRepositoryJpa);
	  final ElementType elType = elementTypeRepositoryJpa.findByName("com.mycompany.customerrelations.Customer");
	  final Element newElement = creator.create(data, elType); 

	  assertEquals( 2, newElement.getProperties().size());
	  assertEquals( 1, newElement.getLastElementRefList("person").getElementList().size());
	  final Map<String,Object> newdata = newElement.toMap();
		newdata.remove("modcomment");
		newdata.remove("changed");
		newdata.remove("type");
		((Map<String, Object>)((List<Object>)newdata.get("person")).get(0)).remove("modcomment");
		((Map<String, Object>)((List<Object>)newdata.get("person")).get(0)).remove("changed");
		((Map<String, Object>)((List<Object>)newdata.get("person")).get(0)).remove("type");
	  assertEquals(data, newdata );
  }

    @Test @Sql({"/gendas-data.sql"})
  public void createElementFromMapWithArray() throws ElementCreationException, CoreException{
	  final Map<String,Object> data = new HashMap<String, Object>();
	  data.put("credits", new ArrayList<Double>());
	  ((List)data.get("credits")).add(1d);
	  ((List)data.get("credits")).add(2d);
	  
	  final ElementCreator creator = new ElementCreator(elementTypeRepositoryJpa,
			  elementRepository, ownerRepository, groupRepository, propertyTypeRepositoryJpa);
	  final ElementType elType = elementTypeRepositoryJpa.findByName("com.mycompany.customerrelations.Customer");
	  final Element newElement = creator.create(data, elType); 

	  assertEquals( 1, newElement.getProperties().size());
	  final Map<String,Object> newdata = newElement.toMap();
		newdata.remove("modcomment");
		newdata.remove("changed");
		newdata.remove("type");
	  assertEquals(data, newdata);
  }
  
  
    @Test @Sql({"/gendas-data.sql"})
  public void createElementFromMapWithRefs() throws ElementCreationException, CoreException, IOException{
	  org.springframework.core.io.Resource resource = new ClassPathResource("/model.json");
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> model = mapper.readValue(resource.getInputStream(), Map.class);
	  
	  final ElementCreator creator = new ElementCreator(elementTypeRepositoryJpa, elementRepository,
              ownerRepository, groupRepository, propertyTypeRepositoryJpa);
	  final ElementType elType = elementTypeRepositoryJpa.findByName("ModelType");
	  final Element newElement = creator.create(model, elType);
	  
      Element element = null;
	  element = new Element(newElement.getElementType());
	  
	  List<Element> knownElements = new ArrayList<Element>();
	  knownElements.add(element);
	  element.assign(newElement, knownElements, elementRepository);
	  final var expected = element.toMap();
		expected.remove("modcomment");
		expected.remove("changed");
		expected.remove("type");
		final var expectedParams = (Map<String, Object>)((List<Object>)expected.get("modelParameters")).get(0);
		expectedParams.remove("modcomment");
		expectedParams.remove("changed");
		expectedParams.remove("type");

		((Map<String, Object>)((List<Object>)expectedParams.get("parameters")).get(0)).remove("modcomment");
		((Map<String, Object>)((List<Object>)expectedParams.get("parameters")).get(0)).remove("changed");
		((Map<String, Object>)((List<Object>)expectedParams.get("parameters")).get(0)).remove("type");
		((Map<String, Object>)((List<Object>)expectedParams.get("parameters")).get(1)).remove("modcomment");
		((Map<String, Object>)((List<Object>)expectedParams.get("parameters")).get(1)).remove("changed");
		((Map<String, Object>)((List<Object>)expectedParams.get("parameters")).get(1)).remove("type");

		final var expectedResults = (Map<String, Object>)((List<Object>)expected.get("results")).get(0);
		expectedResults.remove("modcomment");
		expectedResults.remove("changed");
		expectedResults.remove("type");

		((Map<String, Object>)((List<Object>)expectedResults.get("parameters")).get(0)).remove("modcomment");
		((Map<String, Object>)((List<Object>)expectedResults.get("parameters")).get(0)).remove("changed");
		((Map<String, Object>)((List<Object>)expectedResults.get("parameters")).get(0)).remove("type");
		((Map<String, Object>)((List<Object>)expectedResults.get("parameters")).get(1)).remove("modcomment");
		((Map<String, Object>)((List<Object>)expectedResults.get("parameters")).get(1)).remove("changed");
		((Map<String, Object>)((List<Object>)expectedResults.get("parameters")).get(1)).remove("type");
	  assertEquals(model, expected);
  }


    @Test@Sql({"/gendas-data.sql"})
  public void modifyElementFromMap() throws ElementCreationException, CoreException{
    final ElementCreator creator = new ElementCreator(elementTypeRepositoryJpa,
    		elementRepository, ownerRepository, groupRepository, propertyTypeRepositoryJpa);
    final ElementType elType = elementTypeRepositoryJpa.findByName("com.mycompany.customerrelations.Customer");

    Long id = Long.valueOf(1L);
    final Map<String,Object> data = elementRepository.findById(id).orElse(null).toMap();
    assertEquals("cust1", data.get("name"));
    data.put("name", "cust2");
    Element newElement = creator.create(data, elType);
    assertEquals( id, newElement.getId());
    String name = (String) newElement.getProperty("name").getValue(0);
    assertEquals( name, "cust2");
	  
      data.remove("name");
      newElement = creator.create(data, elType);
      assertEquals( id, newElement.getId());
      assertEquals( 3, newElement.getProperties().size());
      final Map<String,Object> newdata = newElement.toMap();
      assertNull(newdata.get("name"));
      elementRepository.save(newElement);
      Element e = elementRepository.findById(id).orElse(null);
      assertNull( e.getProperty("name"));
  }
    
}
