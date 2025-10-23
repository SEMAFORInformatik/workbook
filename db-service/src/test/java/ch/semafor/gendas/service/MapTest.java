package ch.semafor.gendas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.semafor.gendas.exceptions.CoreException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("jpa")
public class MapTest {

  /*
   * Simple test for exclude keys in the diff function
   */
  @Test
  public void excludeOnwernameTest() {
    Map<String, Object> l = new HashMap<String, Object>();
    Map<String, Object> r = new HashMap<String, Object>();

    l.put("ownername", "Nicolas Mauchle");

    Map<String, Object> diff = Maps.diff(l, r);

    assertEquals(diff.size(), 0);
  }

  @Test
  public void excludeVersionTest() {
    Map<String, Object> l = new HashMap<String, Object>();
    Map<String, Object> r = new HashMap<String, Object>();

    r.put("version", 2);
    l.put("version", 4);

    Map<String, Object> diff = Maps.diff(l, r);

    assertEquals(diff.size(), 0);
  }

  @Test
  public void excludeIdTest() {
    Map<String, Object> l = new HashMap<String, Object>();
    Map<String, Object> r = new HashMap<String, Object>();

    r.put("id", 2);
    Map<String, Object> diff = Maps.diff(l, r);
    assertEquals(diff.size(), 0);

    r.clear();
    l.put("id", 4);

    diff = Maps.diff(l, r);
    assertEquals(diff.size(), 0);

    l.clear();
    l.put("_id", 4);
    r.put("_id", 6);
    diff = Maps.diff(l, r);
    assertEquals(diff.size(), 0);

  }

  @Test
  public void diffIgnoreEmptyTest() {
    Map<String, Object> l = new HashMap<String, Object>();
    Map<String, Object> r = new HashMap<String, Object>();

    // null
    l.put("null", null);
    assertTrue(Maps.diff(l, r).isEmpty());
    assertTrue(Maps.diff(r, l).isEmpty());

    // empty Map
    l.clear();
    l.put("emptyMap", new HashMap<String, Object>());
    assertTrue(Maps.diff(l, r).isEmpty());
    assertTrue(Maps.diff(r, l).isEmpty());

    // Map of empty elements
    l.clear();
    HashMap<String, Object> mapOfEmptyElements = new HashMap<String, Object>();
    mapOfEmptyElements.put("null", null);
    mapOfEmptyElements.put("emptyMap", new HashMap<String, Object>());
    mapOfEmptyElements.put("emptyList", new ArrayList<Object>());
    l.put("mapOfEmptyElements", mapOfEmptyElements);
    assertTrue(Maps.diff(l, r).isEmpty());
    assertTrue(Maps.diff(r, l).isEmpty());

    // empty List
    l.clear();
    l.put("emptyList", new ArrayList<Object>());
    assertTrue(Maps.diff(l, r).isEmpty());
    assertTrue(Maps.diff(r, l).isEmpty());

    // List of empty elements
    l.clear();
    ArrayList<Object> listOfEmptyElements = new ArrayList<Object>();
    listOfEmptyElements.add(null);
    listOfEmptyElements.add(new HashMap<String, Object>());
    listOfEmptyElements.add(new ArrayList<Object>());
    l.put("listOfEmptyElements", listOfEmptyElements);
    assertTrue(Maps.diff(l, r).isEmpty());
    assertTrue(Maps.diff(r, l).isEmpty());
  }

  @Test
  public void mergeValueAddedTest() throws CoreException {
    Map<String, Object> dest = new HashMap<String, Object>();
    Map<String, Object> diff = new HashMap<String, Object>();

    dest.put("key", 3);
    diff.put("key", null);

    Map<String, Object> merged = Maps.merge(dest, diff);
    assertFalse(merged.containsKey("key"));
  }

  @Test
  public void mergeListRemovedTest() throws CoreException {
    Map<String, Object> dest = new HashMap<String, Object>();
    Map<String, Object> diff = new HashMap<String, Object>();

    ArrayList<Object> list = new ArrayList<Object>();
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("key", 3);
    list.add(map);
    list.add(new HashMap<String, Object>());
    list.add(null);
    diff.put("list", list);

    Map<String, Object> merged = Maps.merge(dest, diff);
    ArrayList<Object> l = (ArrayList<Object>) merged.get("list");
    assertEquals(1, l.size());

  }
}
