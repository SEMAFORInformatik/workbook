package ch.semafor.intens.ws.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.semafor.gendas.model.PropertyType;
import ch.semafor.gendas.search.SearchIn;
import ch.semafor.gendas.search.SearchInterval;
import ch.semafor.gendas.search.SearchInterval.Bounds;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.UserService;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("jpa")
public class SearchFactoryTest {

	@MockBean
	ElementService elementService;
	@MockBean
	UserService userService;


  @Test
  public void interval()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Integer upper = 1;
    Integer lower = 0;
    SearchInterval<Object> expected = new SearchInterval<Object>(lower, upper, Bounds.LeftOpen);
    assertEquals(expected,
        SearchFactory.createSearchObject("(0,1]", PropertyType.Type.INTEGER, false));
  }

  @Test
  public void greaterThan()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Integer upper = null;
    Integer lower = 0;
    SearchInterval<Object> expected = new SearchInterval<Object>(lower, upper, Bounds.LeftOpen);
    assertEquals(expected,
        SearchFactory.createSearchObject("(0,]", PropertyType.Type.INTEGER, false));
  }

  @Test
  public void lessEqual()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Integer upper = 1;
    Integer lower = null;
    SearchInterval<Object> expected = new SearchInterval<Object>(lower, upper, Bounds.LeftOpen);
    assertEquals(expected,
        SearchFactory.createSearchObject("(,1]", PropertyType.Type.INTEGER, false));
  }

  @Test
  public void unclosedIntervall()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    assertThrows(IllegalArgumentException.class, () -> {
      SearchFactory.createSearchObject("(,1", PropertyType.Type.INTEGER, false);
    });
  }

  @Test
  public void stringInterval()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    String upper = "Optical";
    String lower = "optical";
    SearchInterval<Object> expected = new SearchInterval<Object>(lower, upper, Bounds.Open);
    assertEquals(expected,
        SearchFactory.createSearchObject("(optical,Optical)", PropertyType.Type.STRING, false));
  }

  @Test
  public void stringInArray()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    String[] opts = new String[]{"One", "Two", "Three"};
    SearchIn<Object> expected = new SearchIn<Object>(opts);
    assertEquals(expected,
        SearchFactory.createSearchObject("{One,Two,Three}", PropertyType.Type.STRING, false));
  }

  @Test
  public void integerInArray()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Integer[] opts = new Integer[]{1, 2, 3};
    SearchIn<Object> expected = new SearchIn<Object>(opts);
    assertEquals(expected,
        SearchFactory.createSearchObject("{1,2,3}", PropertyType.Type.INTEGER, false));
  }

  @Test
  public void doubleInArray()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Double[] opts = new Double[]{1.2, 2.3, 3.4};
    SearchIn<Object> expected = new SearchIn<Object>(opts);
    assertEquals(expected,
        SearchFactory.createSearchObject("{1.2,2.3,3.4}", PropertyType.Type.REAL, false));
  }

  @Test
  public void spacesAfterIntervall()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    String upper = "GENDAS";
    String lower = "gendas";
    SearchInterval<Object> expected = new SearchInterval<Object>(lower, upper, Bounds.Open);
    assertEquals(expected,
        SearchFactory.createSearchObject("(gendas,GENDAS)     ", PropertyType.Type.STRING, false));
  }

  @Test
  public void charactersAfterIntervall()
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    String upper = "GENDAS";
    String lower = "gendas";
    SearchInterval<Object> expected = new SearchInterval<Object>(lower, upper, Bounds.Open);
    assertEquals(expected,
        SearchFactory.createSearchObject("(gendas,GENDAS)TEST  ", PropertyType.Type.STRING, false));
  }

}
