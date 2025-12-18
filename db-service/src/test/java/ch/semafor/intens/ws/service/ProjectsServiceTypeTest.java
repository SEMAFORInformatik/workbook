package ch.semafor.intens.ws.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.ElementCreationException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.UserService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("jpa")
public class ProjectsServiceTypeTest {

	@MockitoBean
	@Qualifier("elementServiceJpa")
	ElementService persistenceService;
	@MockitoBean
	UserService userService;

	@Autowired 
	ProjectsService projectService;
	
	private Map<String, Object> createTestDataMap() throws CoreException{
		Map<String, Object> m = new HashMap<String, Object>();
		m.put( "value", "simplevalue");
		return m;
	}
	
	
	private List<Map<String, Object>> createTestDataList(){
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		HashMap<String, Object> m = new HashMap<String, Object>();
		m.put("name", "Project1");
		m.put("owner", "somebody");
		l.add(m);
		return l;
	}
	@Test
	@WithMockUser(username="admin",roles={"USER","ADMIN"})
	public void findById() throws CoreException, ElementCreationException {
		Map<String, Object> expected = createTestDataMap();
		Mockito.when(persistenceService.getElementMap(1L)).thenReturn(expected);
		Map<String, Object> actual = 
				projectService.findById(1L);

		assertEquals(expected, actual);
		Mockito.verify(persistenceService).getElementMap(1L);
	}
	@Test
    @WithMockUser(username="me",roles={"USER","ADMIN"})
	public void findByName() throws UsernameNotFoundException {
		String projectname="Project1";
		String owner ="me";
		String status="local";
		List<Map<String, Object>> expected = createTestDataList();
		String username=owner;
		Map<String, Object> searchargs = new HashMap<String, Object>();
		searchargs.put("name", new SearchEq<String>(projectname));
		searchargs.put("status", new SearchEq<String>(status));
		Map<String,Map<String, Object>> childsearch = new HashMap<String,Map<String,Object>>();

		Mockito.when(userService.findOwnerByUsername(username)).thenReturn( new Owner(username));
		Mockito.when(persistenceService.findByType(eq(ProjectsService.PROJECT_TYPE), eq(owner), 
				anyList(),
		eq(searchargs), eq(childsearch),eq(-1),eq(-1), eq(null), eq(true), eq(null))).thenReturn(expected);
		
		Mockito.when(persistenceService.getElementType(BaseServiceImpl.PROJECT_TYPE)).thenReturn(
				new ElementType(BaseServiceImpl.PROJECT_TYPE));
		//MultivaluedMap<String, String> qp = new MultivaluedHashMap<String, String>();
		Map<String, String> qp = new HashMap<>();
		qp.put("name", projectname);
		qp.put("owner",owner);
		qp.put("status", status);

		List<Map<String, Object>> actual=projectService.find(qp);
		assertEquals(expected, actual);
		
		Mockito.verify(userService).findOwnerByUsername(owner);
	}

	@Test
    @WithMockUser(username="me",roles={"USER","ADMIN"})
	void findByComponent() throws CoreException {
		String username = "me";
		Mockito.when(userService.findOwnerByUsername(username)).
			thenReturn( new Owner(username));

		Map<String, Object> expected = createTestDataMap();
		Mockito.when(persistenceService.getElementMap(1L)).thenReturn(expected);

		var element = new ElementType(VariantsService.VARIANT_TYPE);
		element.addReference("testRef", element);
		Mockito.when(persistenceService.getElementType(VariantsService.VARIANT_TYPE)).
			thenReturn(element);

		var returnMap = new HashMap<String, Object>();
		returnMap.put("projectId", 1);

		var returnList = new ArrayList<Map<String, Object>>();
		returnList.add(returnMap);

		Mockito.when(persistenceService.findByType(
			eq(VariantsService.VARIANT_TYPE), eq(null), anyList(),
			eq(null), anyMap(), eq(0), eq(0), eq(null), eq(true))
		).
			thenReturn(returnList);
		assertEquals(expected, projectService.findByComponent(1L, null).get(0));
	}

	@Test
    @WithMockUser(username="me",roles={"USER","ADMIN"})
	public void findProjectById() throws UsernameNotFoundException {
		String projectname="Project1";
		String owner ="me";
		String status="local";
		List<Map<String, Object>> expected = createTestDataList();
		String username=owner;

		Mockito.when(userService.findOwnerByUsername(username)).
			thenReturn( new Owner(username));
		Long id = 0L;
		Mockito.when(persistenceService.getElement(eq(id), anyList())).
			thenReturn(expected.get(0));
		Map<String, String> qp = new HashMap<>();
		qp.put("name", projectname);
		qp.put("owner", owner);
		qp.put("id", id.toString());
		qp.put("status", status);

		List<Map<String, Object>> actual=projectService.find(qp);
		assertEquals(expected, actual);
		
		Mockito.verify(userService).findOwnerByUsername(owner);
	}

	@Test
    @WithMockUser(username="me",roles={"USER","ADMIN"})
	void saveProject() throws CoreException {
		String username="me";
		var owner = new Owner(username);
		owner.addGroup(new Group("test"));
		owner.setActiveGroup(new Group("test"));

		Mockito.when(userService.findOwnerByUsername(username)).
			thenReturn( owner );

		// Return the map that the save method gets so we can check if it gets saved with the right values
		Mockito.when(persistenceService.save(anyMap(), eq(VariantsService.PROJECT_TYPE), eq(username), anyString())).
			thenAnswer(i -> i.getArguments()[0]);

		var projectMap = new HashMap<String, Object>();
		projectMap.put("reason", "testReason");

		// A save without any name or id should give an empty return
		assertEquals(Collections.emptyMap(), projectService.save((Map<String, Object>) projectMap.clone()));

		projectMap.put("name", "Test Project");
		projectMap.put("testVariable", "testVal");

		assertEquals("test", projectService.save((Map<String, Object>) projectMap.clone()).get("group"));

		projectMap.put("testVariable", "newTestVal");
		assertEquals("newTestVal", projectService.save((Map<String, Object>) projectMap.clone()).get("testVariable"));
	}	
}
