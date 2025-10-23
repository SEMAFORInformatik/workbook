package ch.semafor.gendas.dao.jpa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import ch.semafor.gendas.dao.OwnerRepository;
import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.ElementCreationException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.Property;
import ch.semafor.gendas.model.PropertyType;
import ch.semafor.gendas.model.PropertyValue;
import ch.semafor.gendas.model.PropertyValueList;
import ch.semafor.gendas.model.TableModification;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.gendas.search.SearchIn;

@ActiveProfiles("jpa")
@DataJpaTest
public class ElementRepositoryJpaTest {

    private static final Logger logger = LoggerFactory.getLogger(ElementRepositoryJpaTest.class);

    @Autowired
    private ElementRepositoryJpa elementRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private ElementTypeRepositoryJpa elementTypeRepository;

    @Autowired
    private PropertyTypeRepositoryJpa propertyTypeRepositoryJpa;

    @Test
    @Sql({"/gendas-data.sql"})
    public void genderCode() throws CoreException {
        Element g = elementRepository.findById(Long.valueOf(5L)).orElse(null);
        PropertyType value= propertyTypeRepositoryJpa.findByElementTypeAndName(
            g.getElementType(),"value");

        assertEquals("female", g.getProperty(value).getValue(0));
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testGetElements() throws CoreException {
        List<Element> l = elementRepository.findAll(); 
        assertEquals(8, l.size(), "Query returned rows");

        Element e=elementRepository.findById(Long.valueOf(1L)).orElse(null);
        l = e.getListOfElements("addresses"); 
        assertNotNull(l);
        assertEquals(1,l.size());
        l = e.getListOfElements("person");
        assertEquals(1,l.size());
        if( logger.isDebugEnabled()){
            e.print( 0, "after select" );
        }
        Element eref1 = l.get( 0 );
        final Property pref1 = eref1.getProperties().get( 0 );
        pref1.setValue( 0, "Haleluia" );

        eref1 = elementRepository.save( eref1 );
        if( logger.isDebugEnabled()){
            e.print( 0, "after Reference update" );
        }
        Element a=elementRepository.findById(Long.valueOf(2L)).orElse(null);
        assertNotNull( a );
        assertEquals(6, a.getProperties().size());
        //assertEquals( a.getParent(), e);
        Element p = elementRepository.findById(Long.valueOf(3L)).orElse(null);

        final PropertyType firstName = propertyTypeRepositoryJpa.findByElementTypeAndName(
            p.getElementType(),"firstname");
        assertNotNull(firstName, "firstname" );
        final PropertyType lastName  = propertyTypeRepositoryJpa.findByElementTypeAndName(
            p.getElementType(),"lastname");
        assertNotNull(lastName, "lastname" );

        final Property pfirst = p.getProperty( firstName );
        assertNotNull( pfirst, "Property firstName" );

        assertEquals( "firstName1", p.getProperty( firstName ).getValue(0));
        assertEquals( "Haleluia", p.getProperty( lastName ).getValue(0));
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testUpdateElement() throws CoreException {
        Element e = elementRepository.findById(Long.valueOf(3L)).orElse(null);

        Property p = e.getProperties().get(0);
        assertTrue( p != null );
        assertEquals( "lastName1", p.getValue(0));

        p.setValue(0, "lastName2");
        p.setValue(1, "lastName11");
        p.setValue(2, "lastName22");
        p.setValue(3, "lastName33");
        e = elementRepository.save(e);

        p = e.getProperties().get(0);
        assertNotNull( p );
        assertEquals( "lastName2", p.getValue(0));
        p.setValue(0, "lastName3");
        // add new Property
        p = new Property( e, new PropertyType("birthday2", PropertyType.Type.DATE) );
        p.setValue(0, new Date());

        e = elementRepository.save(e);

        Element e2 = elementRepository.findById(Long.valueOf(3L)).orElse(null);
        if( logger.isDebugEnabled()){
            e2.print(0,"after second select");
        }
        Property p2 = e2.getProperties().get(0);
        assertEquals( "lastName3", p2.getValue(0));
        p = new Property( e2, new PropertyType("gugus", PropertyType.Type.get("STRING")) );
        p.setValue(0, "ABCDE");
        if( logger.isDebugEnabled()){
            e2.print(0, "before save");
        }
        e = elementRepository.save(e2);
        if( logger.isDebugEnabled()){
            e.print(0, "after save");
        }
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testCrunchElement() throws CoreException {
        Element person = new Element( elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Person") );
        PropertyType plast = propertyTypeRepositoryJpa.findByElementTypeAndName(
            person.getElementType(),"lastname");
        assertEquals("lastname", plast.getName());
        PropertyType pfirst = propertyTypeRepositoryJpa.findByElementTypeAndName(
            person.getElementType(),"firstname");
        assertEquals("firstname", pfirst.getName());
        Property givenName = new Property( person, pfirst);
        Property lastName =  new Property( person, plast );
        PropertyType propType = new PropertyType("birthday");
        Property birthday = new Property( person, propType );

        lastName.setValue(0, "XXXXXX");
        assertTrue(person.getProperties().size()==3);
        if( logger.isDebugEnabled()){
            person.print(0,"before crunch");
        }
        person.crunch();
        if( logger.isDebugEnabled()){
            person.print(0,"after crunch");
        }
        assertEquals(1, person.getProperties().size());
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testModifyElement() throws CoreException {
        Element a = elementRepository.findById(Long.valueOf(2L)).orElse(null); // Address
        Element e = elementRepository.findById(Long.valueOf(1L)).orElse(null); // Customer
        assertNotNull( a );
        PropertyType t = propertyTypeRepositoryJpa.findByName("line1");
        assertNotNull( t );
        Property pa = a.getProperty(t); 
        assertNotNull( pa );
        pa.setValue(0, "newvalue");

        PropertyType trev = propertyTypeRepositoryJpa.findByName("revenue");
        assertNotNull( trev );
        Property prev = e.getProperty(trev); 
        assertNull( prev );
        prev = new Property(e, trev);
        Double num= 123.10;
        prev.setValue(0, num);

        logger.debug("property {}", e.getProperty(trev));
        elementRepository.save(e);


        Element m = elementRepository.findById(Long.valueOf(2L)).orElse(null);

        assertEquals("newvalue", m.getProperty(t).getValue(0));

        m = elementRepository.findById(Long.valueOf(1L)).orElse(null);

        assertEquals(num, m.getProperty(trev).getValue(0));
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testSaveElementWithReferences() throws CoreException {
        final String []cities=new String [] {"city1", "city2", "city3"};
        final List<Long> ids = new ArrayList<Long>();

        for( String name: cities ){
            Element person = new Element(  
                    elementTypeRepository.findByName(
                        "com.mycompany.customerrelations.Person"));
            Element address = new Element(   
                    elementTypeRepository.findByName(
                        "com.mycompany.customerrelations.Address"));
            Property city = new Property(address, 
                propertyTypeRepositoryJpa.findByName("city") );
            city.setValue(0, name);

            List<Element> refs = new ArrayList<Element>(); 
            refs.add(address);
            // System.out.println("adding address");
            person.setListOfElements( "address", refs );
            person = elementRepository.save(person); // SAVE person
            ids.add(person.getId());
            logger.debug( person.toString());
        }

        int i=0; 
        String [] cityNames=new String[cities.length];
        for( Long id: ids){
            Element p2=elementRepository.findById(id).orElse(null); 
            logger.debug( p2.toString());
            assertNotNull(p2.getListOfElements("address"));
            cityNames[i] =(String) p2.getListOfElements("address").get(0).getProperty(
                propertyTypeRepositoryJpa.findByName("city")).getValue(0);
            i++;
        }
        assertArrayEquals(cities, cityNames);
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testRemoveReferences() throws CoreException {
        Element cust = elementRepository.findById(1L).orElse(null);
        List<Element> addresses = cust.getListOfElements("addresses");
        assertEquals(1, addresses.size());
        addresses = new ArrayList<Element>();
        cust.setListOfElements("addresses", addresses);
        cust=elementRepository.save(cust);
        assertEquals(0, cust.getListOfElements("addresses").size());
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testModifyElementWithReferences() throws CoreException {
        Element person = new Element( elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Person"));
        final Property lastname = new Property( person, 
            propertyTypeRepositoryJpa.findByName("lastname") );
        lastname.setValue(0, "Muster" );

        Element address = new Element( elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Address"));
        Property city = new Property( address, 
            propertyTypeRepositoryJpa.findByName("city") );
        city.setValue(0, "Basel" );

        List<Element> refs = new ArrayList<Element>();
        refs.add(address);

        person.setListOfElements("address", refs);
        if( logger.isDebugEnabled()){
            person.print(0, "before first save");
        }
        person = elementRepository.save(person); // SAVE person
        if( logger.isDebugEnabled()){
            person.print(0, "after first save");
        }

        // modify
        final Property firstname = new Property( person, 
            propertyTypeRepositoryJpa.findByName("firstname") );
        firstname.setValue(0, "Max" );
        if( logger.isDebugEnabled()){
            person.print(0, "after insert of Max");
        }
        refs = person.getListOfElements("address");
        assertTrue( refs.size()==1);
        //System.out.println("Step 1");
        address = refs.get(0);
        //System.out.println("Step 2");
        PropertyType land = new PropertyType("Land", PropertyType.Type.get("STRING"));
        propertyTypeRepositoryJpa.save(land);
        final Property country = new Property( address, land );
        country.setValue(0, "Schweiz" );
        //System.out.println("Step 3");
        city = address.getProperty("city");
        city.setValue(0, "Genf" );
        if( logger.isDebugEnabled()){
            person.print(0, "before second save");
        }
        person = elementRepository.save(person); // SAVE person
        if( logger.isDebugEnabled()){
            person.print(0, "after second save");
        }
        Element customer1 = new Element( elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Customer"));
        city = new Property( customer1, propertyTypeRepositoryJpa.findByName("city") );
        city.setValue(0, "Basel" );
        customer1 = elementRepository.save(customer1); // SAVE customer1 with agregation = 1

        Element customer2 = new Element( elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Customer"));
        city = new Property( customer2, propertyTypeRepositoryJpa.findByName("city") );
        city.setValue(0, "Zürich" );
        customer2 = elementRepository.save(customer2); // SAVE customer2 with agregation = 1

        refs = new ArrayList<Element>();
        refs.add(customer1);
        person.setListOfElements("customer", refs);
        person = elementRepository.save(person);
        if( logger.isDebugEnabled()){
            person.print(0, "after third save");
        }

        refs = new ArrayList<Element>();
        refs.add(customer2);
        person.setListOfElements("customer", refs);
        person = elementRepository.save(person);
        if( logger.isDebugEnabled()){
            person.print(0, "after fourth save");
        }
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testUpdateElementWithReferences() throws CoreException {
        Element e = elementRepository.findById(Long.valueOf(1L)).orElse(null);
        if( logger.isDebugEnabled()){
            e.print(0, "print");
        }

        final Element customer = new Element( elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Customer"));
        final Property city = new Property( customer, 
            propertyTypeRepositoryJpa.findByName("city") );
        city.setValue(0, "Basel" );

        final List<Element> refs = new ArrayList<Element>();
        refs.add(customer);
        e.setListOfElements("customer", refs);
        if( logger.isDebugEnabled()){
            e.print(0, "print before save");
        }
        e = elementRepository.save(e);
        if( logger.isDebugEnabled()){
            e.print(0, "print after save");
        }
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testSaveElement() throws CoreException {
        Element person = new Element(elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Person"));
        person.setOwner( ownerRepository.findByUsername("bob"));
        final Property firstName = new Property( person, 
            propertyTypeRepositoryJpa.findByName("firstname"));
        firstName.setValue(0, "firstName2");

        final Property lastName =  new Property( person, 
            propertyTypeRepositoryJpa.findByName("lastname") );
        lastName.setValue(0, "lastName2");

        PropertyType propType = new PropertyType("birthday2",
            PropertyType.Type.get("DATE"));
        propType = propertyTypeRepositoryJpa.save(propType);

        ElementType personType = elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Person");

        elementTypeRepository.save( personType );

        assertNotNull( propType );
        Property prop = new Property( person, propType );
        prop.setValue(0, new Date() );
        assertNull( person.getId());

        List<Element> elements = elementRepository.findAll();

        person = elementRepository.save(person); // SAVE person
        assertNotNull( person.getId() );
        final List<TableModification> histories = person.getModifications();
        assertEquals( 1, histories.size(), "modifications" );

        List<Element> l = elementRepository.findAll(); 
        assertEquals(9, l.size(), "Query returned rows");

        person = elementRepository.findById(person.getId()).orElse(null);
        PropertyType t = propertyTypeRepositoryJpa.findByName("firstname");
        assertEquals("firstName2", person.getProperty(t).getValue(0));
        t = propertyTypeRepositoryJpa.findByName("birthday2");
        assertNotNull(t);
        prop=person.getProperty(t);
        assertNotNull(prop);
    }

    @Test@Sql({"/gendas-data.sql"})
    public void saveNewElement() throws CoreException {
        final ElementType personType = elementTypeRepository.findByName(
            "com.mycompany.customerrelations.Person");
        Element newElement = new Element(personType);
        assertNull(newElement.getId());
        newElement = elementRepository.save(newElement);
        assertNotNull(newElement.getId());
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testRemoveElement() throws CoreException {
        List<Element> l = elementRepository.getAllActive();
        assertEquals( 8, l.size(), "Query returned rows");
        Element e = elementRepository.findById(1L).orElse(null);
        e.setStateDeleted();
        elementRepository.save(e);
        for( TableModification m: e.getModifications()){
            logger.debug( m.toString());
        }
        l = elementRepository.findByType("com.mycompany.customerrelations.Customer");//elementRepository.getAllActive();
        assertEquals( 2, l.size(), "Find by Type returned rows" );
        l = elementRepository.getAllActive();
        assertEquals( 7, l.size(), "Query returned rows");
    }

    @Test@Sql({"/gendas-data.sql"})
    public void deleteElement() throws CoreException {
        Long count = elementRepository.count();
        List<Element> l;
        l=elementRepository.findByType("com.mycompany.customerrelations.Customer");
        long count2 = l.size();
        assertNotNull(elementRepository.findById(1L));
        elementRepository.deleteById(1L);
        assertNull(elementRepository.findById(1L).orElse(null));
        l = elementRepository.findByType("com.mycompany.customerrelations.Customer");
        assertEquals(count2-1, l.size());
        assertEquals(count-1,  elementRepository.count());
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testFindElementsByType() throws CoreException{
        List<Element> l = elementRepository.findByType(
            "com.mycompany.customerrelations.Person");
        assertEquals(1, l.size(), "Query returned rows");
        PropertyType t = propertyTypeRepositoryJpa.findByName("lastname");
        assertEquals("lastName1",
                l.get(0).getProperty(t).getValue(0), "Query returned element with lastname ");
    }

    @Test@Sql({"/gendas-data.sql"})
    public void findCustomerElement() throws CoreException {
        Element e = elementRepository.findById(1L).orElse(null);
        assertNotNull(e);
        assertEquals( 1, e.getElementRefList("friends",
            TableModification.MaxRevision).getElementList().size());
    }

    @Test@Sql({"/gendas-data.sql"})
    public void concurrentUpdate() throws CoreException{
        Element e1 = elementRepository.findById(2L).orElse(null);
        Element e2 = elementRepository.findById(2L).orElse(null);

        final PropertyType t = propertyTypeRepositoryJpa.findByName("line1");
        Property pa = e1.getProperty(t); 
        pa.setValue(0, "newvalue");
        if( logger.isDebugEnabled()){
            logger.debug( e1.getVersion().toString());
        }
        elementRepository.save(e1);
        if( logger.isDebugEnabled()){
            logger.debug( e1.getVersion().toString());
        }

        pa = e2.getProperty(t);
        pa.setValue(0, "othervalue");
        if( logger.isDebugEnabled()){
            logger.debug( e2.getVersion().toString());
        }
        elementRepository.save(e2);
        if( logger.isDebugEnabled()){
            logger.debug( e2.getVersion().toString());
        }
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testFindElementsByOwner() throws CoreException, UsernameNotFoundException {
        String etype= "com.mycompany.customerrelations.Customer";
        Owner owner= ownerRepository.findByUsername("bob");
        // find all customer elements of bob
        Pageable pageable=null;
        boolean latestRevision=true;
        List<Element> elements= elementRepository.findElementsByArgs(
            etype, owner, null, null, pageable, latestRevision, null, false) ;
        assertEquals(1, elements.size());
    }
    @SuppressWarnings("unchecked")
    @Test@Sql({"/gendas-data.sql"})
    public void testFindElementsInArray() throws CoreException {
        String etype= "com.mycompany.customerrelations.Customer";
        Owner owner=null;
        final Map<String, Object> args = new HashMap<String, Object>();
        args.put("customerNumber", new SearchIn(new Integer []{1111,987}));

        // find all customer elements with customerNumber in array
        Pageable pageable=null;
        boolean latestRevision=true;
        List<Element> elements= elementRepository.findElementsByArgs(
            etype, owner, args, null, pageable, latestRevision, null, false) ;
        assertEquals(1, elements.size());
    }

    @SuppressWarnings("unchecked")
    @Test@Sql({"/gendas-data.sql"})
    public void testFindElementsByArgs() throws CoreException {
        String etype= "com.mycompany.customerrelations.Customer";
        if( logger.isDebugEnabled()){
            for( Element e: elementRepository.findByType(etype)){
                e.print(0, "customer");
            }
        }
        Owner owner=null;
        Pageable pageable=null;
        boolean latestRevision=true;
        // find all customer elements
        List<Element> elements= elementRepository.findElementsByArgs(
            etype, owner, null, null,pageable, latestRevision, null, false) ;
        assertEquals(3, elements.size());

        // find customer elements with customerNumber 123
        final Map<String, Object> cust123 = new HashMap<String, Object>();
        cust123.put("customerNumber", new SearchEq(123));
        elements= elementRepository.findElementsByArgs(
            etype, owner, cust123, null, pageable, latestRevision, null, false) ;
        assertEquals(1, elements.size());

        // find customer elements with person and address
        Map<String, Map<String, Object>> persaddr = new HashMap<String, Map<String, Object>>();
        Map<String, Object> personargs = new HashMap<String, Object>();
        personargs.put("lastname", new SearchEq("lastName1"));
        persaddr.put("person", personargs);

        Map<String, Object> addressargs = new HashMap<String, Object>();
        addressargs.put("city", new SearchEq("%ity%"));
        addressargs.put("country", new SearchEq("Country%"));
        persaddr.put("addresses", addressargs);

        elements= elementRepository.findElementsByArgs(
            etype, owner, cust123, persaddr, pageable, latestRevision, null, false) ;
        assertEquals(1, elements.size());

        // find customer elements with customerNumber 2222 and person with lastname
        final Map<String, Object> cust2222 = new HashMap<String, Object>();
        cust2222.put("customerNumber", new SearchEq(2222));
        elements= elementRepository.findElementsByArgs(
            etype, owner, cust2222, persaddr, pageable, latestRevision, null, false) ;
        assertEquals(0, elements.size());

        // find customer elements with address
        elements= elementRepository.findElementsByArgs(
            etype, owner, null, persaddr, pageable, latestRevision, null, false) ;
        assertEquals(2, elements.size());

        // find address elements
        Map<String, Object> addrargs = new HashMap<String, Object>();
        addrargs.put("city", new SearchEq("%ity%"));
        addrargs.put("country", new SearchEq("Country%"));
        elements = elementRepository.findElementsByArgs(
            "com.mycompany.customerrelations.Address",
            owner, addrargs, null, pageable, latestRevision , null, false);
        assertEquals(2, elements.size());

        // find customer elements with friend references to customer 2222
        Map<String, Object> friends = new HashMap<String, Object>();
        friends.put("customerNumber", new SearchEq(2222));
        Map<String, Map<String, Object>> friendargs = new HashMap<String, Map<String, Object>>();
        friendargs.put("friends", friends);
        elements= elementRepository.findElementsByArgs(
            etype, owner, null, friendargs, pageable, latestRevision, null, false) ;
        assertEquals(1, elements.size());

        Element e = elementRepository.findById(1L).orElse(null);
        logger.debug( "Find friends of {}", e.getElementType().getName());
        for( Element f : e.getElementRefList("friends", 
            TableModification.MaxRevision).getElementList()){
            logger.debug( " friend id {} customerNumber {}",f.getId(),
                    f.getProperty("customerNumber").getValue(0));
        }
    }

    @Test@Sql({"/gendas-data.sql"})
    public void testListProperties() throws CoreException{
        List<Double> credits=new ArrayList<Double>();
        for(int i=0; i<4; i++){
            credits.add((double) i);
        }
        Element cust = elementRepository.findById(4L).orElse(null);
        PropertyType t = propertyTypeRepositoryJpa.findByName("credits");
        Property p = cust.getProperty(t);
        assertNull(p);
        p = new Property(cust, t);
        for(int i=0; i<credits.size(); i++){
            p.setValue(i, credits.get(i));
        }
        elementRepository.save(cust);

        cust = elementRepository.findById(4L).orElse(null);
        p = cust.getProperty(t);
        assertNotNull(p);
        List<Double> actual = new ArrayList<Double>();
        Long rev = cust.getLastRevision();
        PropertyValueList pl = p.getValueList(TableModification.MaxRevision);
        for( PropertyValue pv: pl.getValues()){
            actual.add( (Double) pv.getValue());
        }
        assertEquals(credits, actual);
    }

    @Test@Sql({"/gendas-data.sql"})
    public void saveAndRetrieveElementWithCData() throws ElementCreationException, CoreException {
        final Map<String,Object> data = new HashMap<String, Object>();
        data.put("cdataval", "value");

        final ElementType elType = elementTypeRepository.findByName("TestType");
        Element el = new Element(elType);
        PropertyType ptype = propertyTypeRepositoryJpa.findByElementTypeAndName(
            elType, "cdataval");
        Property p = new Property(el, ptype);
        p.setValue(0, "val");

        elementRepository.save(el);

        el=elementRepository.findById(el.getId()).orElse(null);
        assertEquals( 1, el.getProperties().size());
        final Map<String,Object> newdata = el.toMap();
        assertEquals("val", newdata.get("cdataval"));
    }


    /*
     * Use the 'gendas-data-modified' datasource.
     * In this datasource the customer 1 has no person
     * But the person is in the modification history
     */
    @Test@Sql({"/gendas-data-modified.sql"})
    public void findByElementWithModification() throws FileNotFoundException, SQLException {

        String etype = "com.mycompany.customerrelations.Customer";
        Owner owner = null;
        List<String> nopnames = null;
        Map<String, Object> searchargs = null;
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("firstname", new SearchEq<String>("firstName1"));
        Map<String, Map<String, Object>> childargs = new HashMap<String, Map<String, Object>>();
        childargs.put("person", m);
        Pageable pageable = null;
        boolean latestRevision = true;

        List<Element> elements;

        // Get customer with person.name = firstName1
        // only in latest revision
        elements = elementRepository.findElementsByArgs(etype, owner,
                searchargs, childargs, pageable, latestRevision, null, false);
        assertEquals(1, elements.size());

        // over all revisions
        latestRevision = false;
        elements = elementRepository.findElementsByArgs(etype, owner,
                searchargs, childargs, pageable, latestRevision, null, false);

        assertEquals(2, elements.size());

    }

}
