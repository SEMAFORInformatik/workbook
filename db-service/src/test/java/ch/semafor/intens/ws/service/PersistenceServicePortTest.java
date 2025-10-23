package ch.semafor.intens.ws.service;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.ElementCreationException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.service.ElementService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("jpa")
public class PersistenceServicePortTest {

	@Autowired
	ElementService elementService;

	//@Test
	public void findByType() {
		Map<String, Object> search = new HashMap<String,Object>();
		search.put("name", "motor%");
		
		for( int i=0; i<20; i++){
		long startTime = System.currentTimeMillis();
		boolean latestRevision = true;
		List<Map<String, Object>> r= elementService.findByType( "PmMotor", null,
				Arrays.asList(
						"name", "rev", "created", "owner", "applicab"),
                search, null,0,0,null, latestRevision);
		long stopTime = System.currentTimeMillis();
		System.out.println(stopTime-startTime);
		}
	}
	
	public void saveMotor() throws IOException, CoreException, ElementCreationException, UsernameNotFoundException{
		Resource resource = new ClassPathResource("/motor.json");
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> motor = mapper.readValue(resource.getInputStream(), Map.class);
		//System.out.println(userData.toString());
		for( int i=0; i<20; i++){
			Long start = System.currentTimeMillis();
			motor.put("name", "motor" + i);
			elementService.save(motor, "PmMotor", null, "created");
			Long end = System.currentTimeMillis();
			System.out.println( "Elapsed time " + (end-start));
		}
	}
	
}
