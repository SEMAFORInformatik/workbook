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
package ch.semafor.gendas.model;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.model.PropertyType.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ElementTest {

	private static final Logger logger = LoggerFactory.getLogger(ElementTest.class);

	@Test
	public void references() throws CoreException {
		final ElementType tpers = new ElementType("person");
		final ElementType taddr = new ElementType("address");
		final Element person = new Element( tpers );
		assertNull( person.getElementRefs("address") );
		person.addElement("address", new Element(taddr));
		assertNotNull( person.getElementRefs("address"));
	}

  @Test
  public void testEquals() throws CoreException {
    final ElementType tpers = new ElementType("person");
    final ElementType taddr = new ElementType("address");
    final Element person1 = new Element( tpers );
    final Element person2 = new Element( tpers );
    final Element addr1 = new Element(taddr);

    final PropertyType tname = new PropertyType("name", Type.STRING);
    final PropertyType tstreet = new PropertyType("street", Type.STRING);
    final Property name = new Property( person1, tname );
    name.setValue(0, "Alibaba");
    final Property street1 = new Property( addr1, tstreet );
    street1.setValue(0, "street1");

    tpers.add(tname);
    tpers.add(tname);
    assertEquals(1, tpers.getPropertyTypes().size());
    assertNotNull(tpers.getPropertyType("name"));

    assertNotEquals(person1, addr1);
    assertNotEquals(person2, person1);

    final List<Element> refs = new ArrayList<Element>();
    refs.add(addr1);
    person1.setListOfElements( "addr", refs );
    assertNotEquals(person2, person1);
    person2.assign(person1, new ArrayList<Element>(), null);
    assertEquals(person2, person1);
  }

  @Test // TODO check element equals with deleted props and/or refs
  public void testAssign() throws CoreException {
	  final ElementType tpers = new ElementType("person");
	  final ElementType taddr = new ElementType("address");
	  final Element person1 = new Element( tpers ); // person1 vom elementTyp tpers
	  final Element person2 = new Element( tpers );
	  final Element addr1 = new Element(taddr); // addr1 vom elementTyp taddr

	  final PropertyType tname = new PropertyType("name", Type.STRING); // neuer propertyType name
	  final PropertyType tstreet = new PropertyType("street", Type.STRING); //neuer propertyType  street
	  final Property name = new Property( person1, tname ); // neus Property name für person 1
	  name.setValue(0, "Alibaba"); // setze das property name auf Alibaba

	  final Property street1 = new Property( addr1, tstreet ); // neues property street fü addr1
	  street1.setValue(0, "street1");

	  tpers.add(tname);
	  tpers.add(tname);
	  assertEquals(1, tpers.getPropertyTypes().size());
	  assertNotNull(tpers.getPropertyType("name"));

    assertNotEquals(person1, addr1);
    assertNotEquals(person2, person1);

	  final List<Element> refs = new ArrayList<Element>();
	  refs.add(addr1);
	  person2.setListOfElements( "addr", refs );
    assertNotEquals(person2, person1);
	  person1.assign(person2, new ArrayList<Element>(), null);

	  assertEquals( person1.getReferences().size(),person2.getReferences().size());
	  logger.debug("person1 Map {}", person1.toMap());
	  logger.debug("person1 String {}", person1.toString());

	  logger.debug("person2 Map {}", person2.toMap());
	  logger.debug("person2 String {}", person2.toString());
//	  assertTrue( person1.equals(person2));
  }


  @Test
  public void toMap() throws CoreException  {
    final ElementType tpers = new ElementType("person");
    tpers.setBeanVersionId("version");
    tpers.setBeanId("id");
    final ElementType taddr = new ElementType("address");
    final Element person1 = new Element( tpers );
    final Element addr1 = new Element(taddr);

    final PropertyType tname = new PropertyType("name", Type.STRING);
    final PropertyType tname1 = new PropertyType("name1", Type.STRING);
    final PropertyType tlastname = new PropertyType("lastname", Type.STRING);
    final PropertyType tanothername = new PropertyType("anothername", Type.STRING);
    final PropertyType tstreet = new PropertyType("street", Type.STRING);
    final Property name = new Property( person1, tname );
    final Property name1 = new Property( person1, tname1 );
    final Property lastname = new Property( person1, tlastname );
    final Property anothername = new Property( person1, tanothername );
    name.setValue(0, "Alibaba");
    name1.setValue(0, "Alibaba");
    lastname.setValue(0, "Alibaba");
    anothername.setValue(0, "Alibaba");
    final Property street1 = new Property( addr1, tstreet );
    street1.setValue(0, "street1");

    tpers.add(tname);
    tpers.add(tname1);
    tpers.add(tanothername);
    tpers.add(tlastname);

    final List<Element> refs = new ArrayList<Element>();
    refs.add(addr1);
    person1.setListOfElements( "addr", refs );
    person1.setId(99L);
    person1.setVersion(0L);
    //System.out.println(person1.toMap());

    Map<String,Object>expected = new HashMap<String,Object>();
    expected.put("id", 99L);
    expected.put("version", 0L);
    expected.put("name","Alibaba");
    expected.put("lastname", "Alibaba");
    Map<String,Object>expectedAddr = new HashMap<String,Object>();
    expectedAddr.put("street", "street1");
    expected.put("addr", expectedAddr);
    final var actual = person1.toMap(Arrays.asList(
        "name", "lastname", "addr.street"));
    assertEquals(expected, actual);

    expected.put("name1", "Alibaba");
    expected.put("anothername", "Alibaba");
    expected.put("type", tpers.getName());
    List<Map<String,Object>> maplist = new ArrayList<Map<String,Object>>();
    Map<String,Object> m = new HashMap<String,Object>();
    m.put("street", "street1");
    maplist.add(m);
    expected.put("addr", maplist);
    final var actual2 = person1.toMap();
    actual2.remove("changed");
    ((Map<String, Object>)((List<Object>)actual2.get("addr")).get(0)).remove("changed");
    assertEquals(expected, actual2);
  }

  @Test
  public void toMatrixMap() throws CoreException  {
	  final ElementType tmatrix = new ElementType("matrix");
	  final Element m1 = new Element( tmatrix );

	  final PropertyType tintarr = new PropertyType("credits", Type.INTEGER);
	  tmatrix.add( tintarr );

	  final Property intarr = new Property( m1, tintarr );
	  int []mcredits = {11,12,21,22};
	  List<Integer> dims = new ArrayList<Integer>();
	  dims.add(2);
	  dims.add(2);
	  intarr.setDims(dims);
	  for( int i=0; i<mcredits.length; i++){
		  intarr.setValue(i, mcredits[i]);
	  }

	  final Map<String,Object> expected = new HashMap<String, Object>();
	  expected.put("credits", new ArrayList<List<Integer>>());
	  ((List)expected.get("credits")).add(new ArrayList<Integer>());
	  ((List<Integer>)((List)expected.get("credits")).get(0)).add(Integer.valueOf(11));
	  ((List<Integer>)((List)expected.get("credits")).get(0)).add(Integer.valueOf(12));
	  ((List)expected.get("credits")).add(new ArrayList<Integer>());
	  ((List<Integer>)((List)expected.get("credits")).get(1)).add(Integer.valueOf(21));
	  ((List<Integer>)((List)expected.get("credits")).get(1)).add(Integer.valueOf(22));
      expected.put("type", tmatrix.getName());
	  final var actual = m1.toMap();
	  actual.remove("changed");
	  assertEquals(expected , actual);

  }


}
