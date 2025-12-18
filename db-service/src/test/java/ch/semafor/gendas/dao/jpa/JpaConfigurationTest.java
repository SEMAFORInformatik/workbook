package ch.semafor.gendas.dao.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Deprecated? Also has no Assert...
 */
@ActiveProfiles("jpa")
@DataJpaTest
public class JpaConfigurationTest {

	@PersistenceContext
	EntityManager em;

	private static final Logger logger = LoggerFactory.getLogger(JpaConfigurationTest.class);

	@Test
	public void columnMapping() throws InstantiationException, IllegalAccessException{
		Metamodel m = em.getMetamodel();
		for(EntityType type: m.getEntities()){
			String classname = type.getName();
			Query q = em.createQuery("Select o from "+classname+" o");
			int r=q.getFirstResult();
			logger.debug("ok: "+classname);
		}
	}
}
