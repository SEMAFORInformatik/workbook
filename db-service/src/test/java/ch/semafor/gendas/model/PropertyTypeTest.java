package ch.semafor.gendas.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PropertyTypeTest {

	@Test
	public void test() {
		assertEquals( PropertyType.Type.CDATA, PropertyType.Type.get("CDATA"));
		assertEquals( PropertyType.Type.LONG, PropertyType.Type.get("LONG"));
		assertEquals( PropertyType.Type.STRING, PropertyType.Type.get("java.lang.String"));
		assertEquals( "java.lang.String", PropertyType.Type.STRING.getName());
		assertEquals( "CDATA", PropertyType.Type.CDATA.getName());
	}

}
