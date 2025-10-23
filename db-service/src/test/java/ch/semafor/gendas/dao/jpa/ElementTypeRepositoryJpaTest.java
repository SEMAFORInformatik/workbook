package ch.semafor.gendas.dao.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import ch.semafor.gendas.model.ElementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("jpa")
@DataJpaTest
public class ElementTypeRepositoryJpaTest {

    @Autowired
    private ElementTypeRepositoryJpa elementTypeRepository;

    @Test
    @Sql("/gendas-data.sql")
    public void findbByElementTypeNotExisting(){
        // check if no type will be returned if name is not existing
        assertNull(elementTypeRepository.findByName("notexisting"));
    }

    @Test@Sql("/gendas-data.sql")
    public void findbByElementTypeandName() {
        // find an elementtype by his name
        final String type = "com.mycompany.customerrelations.Customer";
        ElementType et = elementTypeRepository.findByName(type);
        assertNotNull(et);
        // check if name is equal to created string
        assertEquals(type, et.getName());
        assertNotNull( elementTypeRepository.findById(et.getId()));
    }

    @Test
    public void saveElementType(){
        // create a new elementtype parent
        ElementType et = new ElementType("parent");
        final ElementType child = new ElementType("child" );
        et.addReference( "child", child );

        // save parent into the db
        ElementType db_et = elementTypeRepository.save(et);

        // check if elementtype is the same and is existing
        assertNotNull(db_et.getId());
        assertEquals(et.getName(), db_et.getName());
        assertTrue("Child is child of parent", et.hasReference("child"));
        assertFalse("Parent is not Child of itself", et.hasReference("parent"));
   }

}