package ch.semafor.gendas.dao.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertNull;

import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.PropertyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("jpa")
@DataJpaTest
public class PropertyTypeRepositoryJpaTest {

    @Autowired
    private ElementTypeRepositoryJpa elementTypeRepositoryJpa;

    @Autowired
    private PropertyTypeRepositoryJpa propertyTypeRepositoryJpa;

    @Test
    @Sql("/gendas-data.sql")
    public void findbByElementTypeandNameWithException(){
        ElementType et = elementTypeRepositoryJpa.findByName("com.mycompany.customerrelations.Customer");
        PropertyType pt = propertyTypeRepositoryJpa.findByElementTypeAndName(et, "firstname");
        assertNull("firstname not found", pt);
    }

    @Test@Sql("/gendas-data.sql")
    public void findbByElementTypeandName(){
        ElementType et = elementTypeRepositoryJpa.findByName("com.mycompany.customerrelations.Customer");
        PropertyType pt = propertyTypeRepositoryJpa.findByElementTypeAndName(et, "customerNumber");
        assertEquals("customerNumber", pt.getName());
    }

    @Test@Sql("/gendas-data.sql")
    public void findbByName(){
        PropertyType pt = propertyTypeRepositoryJpa.findByName("customerNumber");
        assertEquals("customerNumber", pt.getName());
    }
    @Test@Sql("/gendas-data.sql")
    public void findbByNameAndUnit(){
        PropertyType pt = propertyTypeRepositoryJpa.findByNameAndUnit("weight", "kg");
        assertEquals("weight", pt.getName());
        assertNull("missing weight in g",
                propertyTypeRepositoryJpa.findByNameAndUnit("weight", "g"));
    }

    @Test
    public void save(){
        PropertyType pt = new PropertyType("property-type", PropertyType.Type.get("INTEGER"));
        propertyTypeRepositoryJpa.save(pt);
        assertNotNull("id is set", pt.getId());
    }
}
