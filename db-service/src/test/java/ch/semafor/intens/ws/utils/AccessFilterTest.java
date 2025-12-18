package ch.semafor.intens.ws.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.UserService;
import ch.semafor.intens.ws.config.ComponentProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("jpa")
public class AccessFilterTest {

  @Autowired
  ComponentProperties componentProperties;

	@MockitoBean
	ElementService elementService;
	@MockitoBean
	UserService userService;

  @Test
  public void filterComponents() {
    Owner owner = new Owner("me");
    Set<Group> memberOf = new HashSet<Group>();
    memberOf.add(new Group("group1"));
    owner.setGroups(memberOf);

    AccessFilter accessFilter = new AccessFilter(owner, componentProperties);
    List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
    Map<String, Object> e = new HashMap<String, Object>();
    e.put("name", "comp1");
    e.put("owner", "you");
    e.put("approval", "inPreparation");
    e.put("group", "group1");
    l.add(e);
    List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>(l);
    assertEquals(expected, accessFilter.filterComponents(l));

    // add another component having group2 in inPreparation
    e = new HashMap<String, Object>();
    e.put("name", "comp2");
    e.put("owner", "you");
    e.put("approval", "inPreparation");
    e.put("group", "group2");
    l.add(e);

    // add another component having group2 in shared
    e = new HashMap<String, Object>();
    e.put("name", "comp3");
    e.put("owner", "you");
    e.put("approval", "shared");
    e.put("group", "group2");
    l.add(e);
    assertEquals(expected, accessFilter.filterComponents(l));
  }

  @Test
  public void filterVariants() {
    Owner owner = new Owner("me");
    Set<Group> memberOf = new HashSet<Group>();
    memberOf.add(new Group("group1"));
    owner.setGroups(memberOf);

    AccessFilter accessFilter = new AccessFilter(owner, componentProperties);
    List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
    Map<String, Object> e = new HashMap<String, Object>();
    e.put("name", "var1");
    e.put("owner", "you");
    e.put("approval", "inPreparation");
    e.put("group", "group1");
    l.add(e);
    List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>(l);
    assertEquals(expected, accessFilter.filterVariants(l));

    // add another variant having group2 in shared
    e = new HashMap<String, Object>();
    e.put("name", "var3");
    e.put("owner", "you");
    e.put("approval", "shared");
    e.put("group", "group2");
    l.add(e);
    expected = new ArrayList<Map<String, Object>>(l);

    // add another element having group2 in inPreparation
    e = new HashMap<String, Object>();
    e.put("name", "var2");
    e.put("owner", "you");
    e.put("approval", "inPreparation");
    e.put("group", "group2");
    l.add(e);

    assertEquals(expected, accessFilter.filterVariants(l));
  }

  @Test
  public void filterProjects() {
    Owner owner = new Owner("me");
    Set<Group> memberOf = new HashSet<Group>();
    memberOf.add(new Group("group1"));
    owner.setGroups(memberOf);

    AccessFilter accessFilter = new AccessFilter(owner, componentProperties);
    List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
    Map<String, Object> e = new HashMap<String, Object>();
    e.put("name", "proj1");
    e.put("owner", "you");
    e.put("status", "local");
    e.put("group", "group1");
    l.add(e);
    List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>(l);
    assertEquals(expected, accessFilter.filterProjects(l));

    // add another element having group2 in shared
    e = new HashMap<String, Object>();
    e.put("name", "proj3");
    e.put("owner", "you");
    e.put("status", "global");
    e.put("group", "group2");
    l.add(e);
    expected = new ArrayList<Map<String, Object>>(l);

    // add another element having group2 in inPreparation
    e = new HashMap<String, Object>();
    e.put("name", "proj2");
    e.put("owner", "you");
    e.put("status", "local");
    e.put("group", "group2");
    l.add(e);

    assertEquals(expected, accessFilter.filterProjects(l));
  }

  @Test
  public void copyComponent() {
    Owner owner = new Owner("me");
    Set<Group> memberOf = new HashSet<Group>();
    memberOf.add(new Group("group1"));
    owner.setGroups(memberOf);

    AccessFilter accessFilter = new AccessFilter(owner, componentProperties);
    // different owner, same group
    Map<String, Object> compmap = new HashMap<String, Object>();
    compmap.put("name", "comp1");
    compmap.put("owner", "you");
    compmap.put("group", "group1");
    assertTrue(accessFilter.isCreationAllowed(compmap));

    // different owner and group
    compmap = new HashMap<String, Object>();
    compmap.put("name", "comp1");
    compmap.put("owner", "you");
    compmap.put("group", "group2");
    assertFalse(accessFilter.isCreationAllowed(compmap));

    // same owner
    compmap = new HashMap<String, Object>();
    compmap.put("name", "comp1");
    compmap.put("owner", "me");
    compmap.put("group", "group1");
    assertTrue(accessFilter.isCreationAllowed(compmap));
  }
}
