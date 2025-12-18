package ch.semafor.intens.ws.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.ElementCreationException;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.UserService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("jpa")
public class VariantsServiceTypeTest {

	@MockitoBean
	@Qualifier("elementServiceJpa")
	ElementService persistenceService;
	@MockitoBean
	UserService userService;

	@Autowired
	VariantsService variantService;

	private static final Logger logger = LoggerFactory.getLogger(VariantsServiceTypeTest.class);

	@Test
	@WithMockUser(username = "admin", roles = { "USER", "ADMIN" })
	public void renameMultiple() throws CoreException, ElementCreationException {
		Owner owner = new Owner("admin");
		Mockito.when(userService.findOwnerByUsername("admin")).thenReturn(owner);
		var saveCounter = new Object() { int value; };
		var testVariants = new ArrayList<Map<String, Object>>();
		testVariants.add(new HashMap<String, Object>() {
			{
				put("id", 1L);
				put("name", "Variant1");
				put("type", "Variant");
				put("owner", "admin");
				put("group", "admin");
				put("projectId", 1);
			}
		});
		testVariants.add(new HashMap<String, Object>() {
			{
				put("id", 2L);
				put("name", "Variant1");
				put("type", "Variant");
				put("owner", "admin");
				put("group", "admin");
				put("projectId", 1);
			}
		});
		testVariants.add(new HashMap<String, Object>() {
			{
				put("id", 3L);
				put("name", "Variant2");
				put("type", "Variant");
				put("owner", "admin");
				put("group", "admin");
				put("projectId", 1);
			}
		});
		Mockito
				.when(persistenceService.findByType(Mockito.anyString(), Mockito.any(),
						Mockito.anyList(), Mockito.anyMap(),
						Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyBoolean()))
				.thenAnswer(args -> {
					Map<String, Object> searchargs = args.getArgument(3);
					var name = (SearchEq) searchargs.get("name");
					return testVariants.stream().filter(e -> e.get("name").equals(name.toString())).collect(Collectors.toList());
				});
		Mockito
				.when(persistenceService.getElementMap(1L))
				.thenReturn(testVariants.get(0));

		Mockito
				.when(persistenceService.save(Mockito.anyMap(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenAnswer(arg0 -> {
					saveCounter.value++;
					return null;
				});

		variantService.rename(1L, "NewVariant", "rename");
		assertEquals(2, saveCounter.value);

	}
}
