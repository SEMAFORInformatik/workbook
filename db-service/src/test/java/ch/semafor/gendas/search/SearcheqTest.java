package ch.semafor.gendas.search;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearcheqTest {

	@Test
	public void criteriaWithInstace(){
		Criteria crit = new Criteria();
		String str = "Some String";
		assertEquals(setCriteria(new Criteria(), str),
				crit.regex(str.replace("%", ".*")));
	}
	
	public Criteria setCriteria(Criteria crit, Object obj) {
		if (obj instanceof String) {
			if (((String) obj).contains("%")) {
				return crit.regex(((String) obj).replace("%", ".*"));
			}
		}
		return crit.is(obj);
	}
}
