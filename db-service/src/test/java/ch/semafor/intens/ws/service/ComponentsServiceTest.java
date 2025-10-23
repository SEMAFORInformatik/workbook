package ch.semafor.intens.ws.service;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.PropertyType;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.UserService;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.model.Component;
import ch.semafor.intens.ws.utils.DateTimeFormatter;
import ch.semafor.intens.ws.utils.IntensWsException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@SpringBootTest
@ActiveProfiles("jpa")
public class ComponentsServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(ComponentsServiceTest.class);

	@MockBean
	@Qualifier("elementServiceJpa")
	ElementService elementService;
	@MockBean
	UserService userService;

  @Autowired
  ComponentsService componentsService;
  @Autowired
  StatelessKieSession kieSession;
  @Autowired
  ComponentProperties componentProperties;

	private ElementType getElementType(String typename){
		ElementType et = new ElementType(typename);
		et.add(new PropertyType("date",PropertyType.Type.DATE));
		et.add(new PropertyType("dateTime",PropertyType.Type.DATE));
		et.add(new PropertyType("dateList",PropertyType.Type.DATE));
		ElementType st = new ElementType("Subtype");
		st.add(new PropertyType("date",PropertyType.Type.DATE));
		et.addReference("subtype", st);
		et.addReference("subtypeList", st);
		return et;
	}
	
	private List<ElementType> createTypeList(String [] names){
		List<ElementType> l = new ArrayList<ElementType>();
		for( String n :names){
			l.add(new ElementType("test"));
		}
		return l;
	}
	private List<Map<String,Object>> toMap( List<ElementType> el ){
		List<Map<String, Object>> types = new ArrayList<Map<String, Object>>();
		for (ElementType et : el) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("name", et.getName());
			m.put("id", et.getId());
			m.put("propertyTypes", new ArrayList<String>());
			for (PropertyType p : et.getPropertyTypes()) {
				Map<String, Object> mp = new HashMap<String, Object>();
				mp.put("name", p.getName());
				mp.put("type", p.getType());
				if (p.getUnit() != null && !p.getUnit().isEmpty()) {
					mp.put("unit", p.getUnit());
				}
				((List) m.get("propertyTypes")).add(mp);
			}
			m.put("references", new ArrayList<String>());
			for (String refname : et.getReferences().keySet()) {
				Map<String, Object> mr = new HashMap<String, Object>();
				mr.put("name", refname);
				mr.put("type", et.getElementType(refname).getName());
				((List) m.get("references")).add(mr);
			}
			types.add(m);
		}
		return types;
	}

//	@Before
//	public void setup() {
//	  MockitoAnnotations.initMocks(this);
//	  componentsService.setSession(kieSession);
//	}

	@Test
    @WithMockUser(username="me",roles={"USER","ADMIN"})	
	public void getAllTypes() {
		String [] typenames = {"type1", "type2"};
		List<ElementType> types = createTypeList(typenames);
		Mockito.when(elementService.getAllElementTypes()).thenReturn(types);
		List<Map<String,Object>> expected = toMap(types);
		List<Map<String,Object>> actual = componentsService.getTypes();
		Mockito.verify(elementService).getAllElementTypes();
		assertEquals(expected, actual);
	}

	@Test
	public void fixDate() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> fixDateMap = mapper.readValue(new File("src/test/resources/fixDate.json"), Map.class);
		Date expectedDate = DateTimeFormatter.convert( (String) fixDateMap.get("date"));
		Date expectedDateTime = DateTimeFormatter.convert( (String) fixDateMap.get("dateTime"));
		Mockito.when(elementService.getElementType("FixDate")).thenReturn(getElementType("FixDate"));
		Mockito.when(elementService.getElementType("Subtype")).thenReturn(getElementType("Subtype"));
		componentsService.fixDate(fixDateMap, "FixDate");
		assertEquals( expectedDate,     fixDateMap.get("date"));
		assertEquals( expectedDateTime, fixDateMap.get("dateTime"));
		assertEquals( expectedDate,     ((ArrayList<Date>)fixDateMap.get("dateList")).get(0));
		assertEquals( expectedDateTime, ((ArrayList<Date>)fixDateMap.get("dateList")).get(1));
		assertEquals( expectedDate,     ((Map<String, Object>) fixDateMap.get("subtype")).get("date"));
		assertEquals( expectedDate,     ((Map<String, Object>) ((ArrayList<Date>) fixDateMap.get("subtypeList")).get(0)).get("date"));
		assertEquals( expectedDateTime, ((Map<String, Object>) ((ArrayList<Date>) fixDateMap.get("subtypeList")).get(1)).get("date"));
	}

	@Test
    @WithMockUser(username="tester",roles={"USER","ADMIN"}) 
	public void findComponent() throws IOException, UsernameNotFoundException{
		String username = "tester";
		String type = "PmMotor";
		List<Map<String,Object>> expected = load("/motors.json"); 
		
		Map<String, Object> searchargs = new HashMap<String, Object>();
		Map<String,Map<String, Object>> childargs = new HashMap<String,Map<String,Object>>();
		Owner owner = new Owner(username);
		Mockito.when(userService.findOwnerByUsername(username)).thenReturn( owner);

        Map<String, String> qp = new HashMap<>();
//		qp.put("name", compname);
//		qp.put("owner", Arrays.asList(new String []{owner}));
//		qp.put("approval", Arrays.asList(new String []{approval}));

//        HttpServletRequest hsr = Mockito.mock(HttpServletRequest.class);
//        Mockito.when(hsr.getParameterMap()).thenReturn(qp);

		ElementType pmtype = new ElementType( type );
		Mockito.when(elementService.getElementType(type)).thenReturn(pmtype);
		Mockito.when(elementService.findByType(eq(type), eq(null), anyList(),
				eq(searchargs), eq(childargs),eq(-1),eq(-1), eq(null), eq(true), eq(null))).thenReturn(expected);
		
		List<Map<String,Object>> actual = componentsService.findByType(type,qp);
		assertEquals(expected, actual);
	}
	
	private List<Map<String,Object>> load( String filename ) throws IOException{
    Resource resource = new ClassPathResource(filename);
    ObjectMapper mapper = new ObjectMapper();
    String type = "PmMotor";
    return mapper.readValue(resource.getInputStream(), 	  
        new TypeReference<List<Map<String,Object>>>() {});
	}
	
	@Test
    @WithMockUser(username="tester",roles={"USER","ADMIN"}) 
	public void saveComponent() throws IOException, UsernameNotFoundException{
    String username = "tester";
    List<Map<String,Object>> motors = load("/motors.json"); 
    // expect findOwner
    Owner owner = new Owner(username);
    owner.addGroup(new Group("activeGroup"));
    owner.setActiveGroup(new Group("activeGroup"));
    Mockito.when(userService.findOwnerByUsername(username)).thenReturn( owner);
    
    // expect findByType
    Map<String, Object> search = new HashMap<String, Object>();
    search.put("name", new SearchEq<String>((String) motors.get(0).get("name")) );
    search.put("rev", new SearchEq<Integer>((Integer) motors.get(0).get("rev")) );
    final String ownername = null;
    final int page=0;
    final int pagesize=-1;
    final String type = (String) motors.get(0).get("type");
    Map<String, Integer> sortmap = new HashMap<String, Integer>();
    sortmap.put("rev", 1);
    boolean latestRevision = true;
    Mockito.when(elementService.findByType(type, ownername,
        Collections.emptyList(), search, null, page, pagesize, sortmap, latestRevision, null)).thenReturn(
            new ArrayList<Map<String,Object>>() );

    HashMap<String, Object> dbMotor = new HashMap<String, Object>();
    dbMotor.put("owner", motors.get(0).get("owner"));
    dbMotor.put("group", motors.get(0).get("group"));
    Long dbId = ((Number) motors.get(0).get("id")).longValue();
    Mockito.when(elementService.getElementMap(dbId)).thenReturn(dbMotor);
    
    // execute save
    motors.get(0).put("reason", "save");
    try {
    	componentsService.save((HashMap<String, Object>) motors.get(0));
    	fail("Creation not allowed expected");
    } catch (IntensWsException e) {
    	assertEquals(400, e.getStatusCode());
    }

    motors.get(0).put("id", null);
    motors.get(0).put("reason", "save");
  	componentsService.save((HashMap<String, Object>) motors.get(0));
	}

  @Test
  @WithMockUser(username="tester",roles={"USER","ADMIN"}) 
  public void renameComponentWrongOwner() throws IOException, UsernameNotFoundException{
    String username = "tester";
    List<Map<String,Object>> motors = load("/motors.json");
    logger.info(motors.get(0).get("id").getClass().toString());
    motors.get(0).put("id", Long.valueOf((Integer) motors.get(0).get("id")));
    motors.get(1).put("id", Long.valueOf((Integer) motors.get(1).get("id")));
    Owner owner = new Owner(username);
    assertThrows(IntensWsException.class, ()->{
      Mockito.when(userService.findOwnerByUsername(username)).thenReturn(owner);
      Mockito.when(elementService.getElementMap(1L)).thenReturn(motors.get(0));
      Mockito.when(elementService
          .checkVersion(1L, ((Number) motors.get(0).get("version")).longValue(), null))
          .thenReturn(true);

      componentsService.rename(1L, "newname", null);
    });
  }

  @Test//(expected=IntensWsException.class)
  @WithMockUser(username="john",roles={"USER","ADMIN"}) 
  public void renameComponent() throws IOException, UsernameNotFoundException, CoreException{
    String username = "john";
    List<Map<String,Object>> motors = load("/motors.json");
    Owner owner = new Owner(username);
    Mockito.when(userService.findOwnerByUsername(username)).thenReturn( owner );
    Mockito.when(elementService.getElementMap(1L)).thenReturn( motors.get(0));
    Mockito.when(elementService.checkVersion(1L, ((Number)motors.get(0).get("version")).longValue(), null)).thenReturn( true);
//    Mockito.when(elementService.getElement(Long.valueOf(1L), Arrays.asList(new String[]{"version"}))).thenReturn(motors.get(0));

    Map<String, Object> search = new HashMap<String, Object>();
    search.put("name", new SearchEq<String>("newname"));
    search.put("rev", new SearchEq<Integer>(0));
    final String anyowner = null;
    final int page=0;
    final int pagesize=-1;
    Map<String, Integer> sortmap = new HashMap<String, Integer>();
    sortmap.put("rev", 1);
    boolean latestRevision = true;
    Mockito.when(elementService.findByType("PmMotor", anyowner,
        Collections.emptyList(), search, null, page, pagesize, sortmap, latestRevision, null))
        .thenReturn(new ArrayList<Map<String,Object>>());
    
    componentsService.rename(1L, "\"newname\"", null);
    Component expected = new Component(motors.get(0), componentProperties);
    expected.setName("newname");
    //Mockito.verify(elementService).save(expected.toMap(), "PmMotor", username, "rename");
  }
  
  @Test
  @WithMockUser(username="tester",roles={"USER","ADMIN"}) 
  public void deleteComponentWrongOwner() throws IOException, UsernameNotFoundException{
    String username = "tester";
    List<Map<String,Object>> motors = load("/motors.json"); 
    Owner owner = new Owner(username);
    assertThrows(IntensWsException.class, ()-> {
      Mockito.when(userService.findOwnerByUsername(username)).thenReturn(owner);
      Mockito.when(elementService.getElementMap(1L)).thenReturn(motors.get(0));
      componentsService.delete(1L);
    });
  }

  @Test
  @WithMockUser(username="me",roles={"USER","ADMIN"}) 
  public void deleteComponentWrongApprovalState() throws IOException, UsernameNotFoundException{
    String username = "me";
    Long id = 2L;
    List<Map<String,Object>> motors = load("/motors.json"); 
    Owner owner = new Owner(username);
    assertThrows(IntensWsException.class, () -> {
      Mockito.when(userService.findOwnerByUsername(username)).thenReturn(owner);
      Mockito.when(elementService.getElementMap(id)).thenReturn(motors.get(1));
      ElementType et = new ElementType("dummy");
      et.addReference("refName", new ElementType("pmMotor"));
      Mockito.when(elementService.getElementType(VariantsService.VARIANT_TYPE)).thenReturn(et);
      componentsService.delete(id);
    });
  }

  @Test
  @WithMockUser(username="john",roles={"USER","ADMIN"}) 
  public void deleteComponentOK() throws IOException, UsernameNotFoundException, CoreException{
    String username = "john";
    Long id = 1L;
    List<Map<String,Object>> motors = load("/motors.json"); 
    Owner owner = new Owner(username);
    
    Mockito.when(userService.findOwnerByUsername(username)).thenReturn( owner);
    Mockito.when(elementService.getElementMap(id)).thenReturn( motors.get(0) );
    ElementType et = new ElementType("dummy");
    et.addReference("refName", new ElementType("pmMotor"));
    Mockito.when(elementService.getElementType(VariantsService.VARIANT_TYPE)).thenReturn(et);
    String anyowner=null;
    List<String> nopnames=null;
    Map<String, Object> search = null;
    Map<String,Map<String, Object>> varcompsearch = new HashMap<String, Map<String,Object>>();
    Map<String, Object>m = new HashMap<String, Object>();
    m.put("compId", new SearchEq<Integer>(id.intValue())); 
    varcompsearch.put("refName", m);
    boolean latestRevision = false;
    Mockito.when(elementService.findByType(
        VariantsService.VARIANT_TYPE, anyowner,nopnames,
        search, varcompsearch, 0, 0, null, latestRevision, null)).thenReturn(new ArrayList<Map<String,Object>>());
    
    componentsService.delete(id);

    Mockito.verify(elementService).deleteElement(id);
  }


  @Test 
  @WithMockUser(username="tester",roles={"USER"})
  public void updateComponentFail() throws UsernameNotFoundException, IOException {
      String username = "tester";
      List<Map<String,Object>> motors = load("/motors.json");
      // expect findOwner
      Owner owner = new Owner(username);
      owner.addGroup(new Group("activeGroup"));
      owner.setActiveGroup(new Group("activeGroup"));
      Mockito.when(userService.findOwnerByUsername(username)).thenReturn( owner);

      Mockito.when(elementService.checkVersion(1L, ((Number)motors.get(0).get("version")).longValue(), "PmMotor")).thenReturn( true);
      
      // expect findByType
      Map<String, Object> search = new HashMap<String, Object>();
      search.put("name", new SearchEq<String>((String) motors.get(0).get("name")) );
      search.put("rev", new SearchEq<Integer>((Integer) motors.get(0).get("rev")) );
      final String ownername = null;
      final int page=0;
      final int pagesize=-1;
      final String type = (String) motors.get(0).get("type");
      Map<String, Integer> sortmap = new HashMap<String, Integer>();
      sortmap.put("rev", 1);
      boolean latestRevision = true;
      Mockito.when(elementService.findByType(type, ownername,
          Collections.emptyList(), search, null, page, pagesize, sortmap, latestRevision, null)).thenReturn(
              new ArrayList<Map<String,Object>>() );

      // Expect getElementMap
      HashMap<String, Object> dbMotor =  (HashMap<String, Object>) motors.get(0);

      Long dbId = ((Number) motors.get(0).get("id")).longValue();
      boolean withDbInfos = false;
      Mockito.when(elementService.getElementMap(dbId, withDbInfos)).thenReturn(dbMotor);
      
      // execute update
      String expected_desc = "New Description";
      String expected_name = "New Name";
      
      HashMap<String, Object> motor = new HashMap<String, Object>();

      motor.put("desc", expected_desc);
      motor.put("name", expected_name);
      motor.put("reason", "save");

      // Mock get modifications
      Map<String, Object> modifications = new HashMap<String, Object>();
      modifications.put("name", Arrays.asList(motor.get("name"), expected_name) );
      
      Map<String, Object> updated_motor = (HashMap<String, Object>) motor.clone();
//      Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime((String)updated_motor.get("created"));
//      updated_motor.put("created", cal.getTime());
      updated_motor.remove("type");
      updated_motor.remove("reason");

      updated_motor.put("rev", 0);
      updated_motor.put("id", 1L);
      updated_motor.put("approval", "inPreparation");
      updated_motor.put("name", "New Name");
      updated_motor.put("version", 0);
      updated_motor.put("owner", "john");
      updated_motor.put("group", "group1");
      updated_motor.put("desc", "New Description");
      Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime("2001-02-03T23:00:12");
      updated_motor.put("created", cal.getTime());

      Mockito.when(elementService.getModifiedProperties(updated_motor)).thenReturn(modifications);

      HashMap<String, Object> motor_wr = (HashMap<String, Object>) motor.clone();

      try {
          componentsService.update(motor_wr, 1L);
          fail("Type missing should thrown");
      } catch (IntensWsException e) {
          assertEquals(400, e.getStatusCode());
      }

      HashMap<String, Object> motor_wt = (HashMap<String, Object>) motor.clone();

      motor_wt.put("type", "PmMotor");

      
      try {
          componentsService.update(motor_wt, 1L);
          fail("Apply rules failed Permission denied");
      } catch (IntensWsException e) {
          assertEquals(400, e.getStatusCode());
      }
      
      
      HashMap<String, Object> motor_wu = (HashMap<String, Object>) motor.clone();
      motor_wu.put("owner", "tester");

      try {
          componentsService.update(motor_wu, 1L);
          fail("Apply rules failed: You can not modify component if it is not yours");
      } catch (IntensWsException e) {
          assertEquals(400, e.getStatusCode());
      }
      Mockito.verify(elementService, times(2)).getModifiedProperties(updated_motor);

  }
  
  @Test 
  @WithMockUser(username="john",roles={"USER","ADMIN"}) 
  public void updateComponentSuccess() throws UsernameNotFoundException, IOException, ParseException {
      String username = "john";
      List<Map<String,Object>> motors = load("/motors.json");
      HashMap<String, Object> motor = (HashMap<String, Object>) motors.get(0);
      // expect findOwner
      Owner owner = new Owner(username);
      owner.addGroup(new Group("group1"));
      owner.setActiveGroup(new Group("group1"));
      Mockito.when(userService.findOwnerByUsername(username)).thenReturn( owner);
      
      Mockito.when(elementService.checkVersion(1L, ((Number)motors.get(0).get("version")).longValue(), "PmMotor")).thenReturn( true);
      
      // expect findByType
      Map<String, Object> search = new HashMap<String, Object>();
      search.put("name", new SearchEq<String>((String) motors.get(0).get("name")) );
      search.put("rev", new SearchEq<Integer>((Integer) motors.get(0).get("rev")) );
      final String ownername = null;
      final int page=0;
      final int pagesize=-1;
      final String type = (String) motors.get(0).get("type");
      Map<String, Integer> sortmap = new HashMap<>();
      sortmap.put("rev", 1);
      boolean latestRevision = true;
      Mockito.when(elementService.findByType(type, ownername,
          Arrays.asList(new String[] {}), search, null, page, pagesize, sortmap, latestRevision, null)).thenReturn(
              new ArrayList<Map<String,Object>>() );

      // Expect getElementMap
      HashMap<String, Object> dbMotor =  (HashMap<String, Object>) motor.clone(); //(HashMap<String, Object>) motors.get(0);

      Long dbId = ((Number) motors.get(0).get("id")).longValue();
      Mockito.when(elementService.getElementMap(dbId)).thenReturn((Map<String, Object>) dbMotor.clone());
      
      // execute update
      String expected_desc = "New Description";
      String expected_name = "New Name";
      
      
      
      HashMap<String, Object> m = new HashMap<String, Object>();

      m.put("desc", expected_desc);
      m.put("name", expected_name);
      m.put("reason", "save");
      
      
      HashMap<String, Object> motor_wt = (HashMap<String, Object>) m.clone();
      
      motor_wt.put("type", "PmMotor");
      
      // Get modifications
      Map<String, Object> modifications = new HashMap<>();
      modifications.put("name", Arrays.asList(motor.get("name"), expected_name) );

      Map<String, Object> updated_motor = motor;
      updated_motor.put("name", expected_name);
      updated_motor.put("desc", expected_desc);
      Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime((String)motor.get("created"));
      updated_motor.put("created", cal.getTime());
      updated_motor.remove("type");
      updated_motor.put("id", 1L);
      
      Mockito.when(elementService.getModifiedProperties(updated_motor)).thenReturn(modifications);
      
      // Save 
      componentsService.update(motor_wt, 1L);

  }
}
