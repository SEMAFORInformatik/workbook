package ch.semafor.gendas.repldbfunc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

public class jdSortTest<T> {
	
	/*private void setUp_nr(){
		reloadClazzes();
		this.act = new ArrayList<Map<String,Object>>();
		this.com = new ArrayList<Map<String,Object>>();
		
		for(int i = 0;i<10;i++){
			this.com.add(i, (Map<String, Object>) new HashMap<String,Object>().put("ints", i));
			this.act.add(i, (Map<String, Object>) new HashMap<String,Object>().put("ints", i));
		}
	}*/
	/*
	private void setUp_str(){
		reloadClazzes();
		act = new ArrayList<Map<String,Object>>();
		com = new ArrayList<Map<String,Object>>();
		
		String[] alphabet = { "a","b","c","d","e","f","g","h","i","j","k","l","m",
				              "n","o","p","q","r","s","t","u","v","w","x","y","z" };
		for(int alp=0;alp<alphabet.length;alp++){
			act.add((Map<String, Object>) new HashMap<String,Object>().put("strings", alphabet[alp]));
			com.add((Map<String, Object>) new HashMap<String,Object>().put("strings", alphabet[alp]));
		}
	}

	private void setUp_double(){
		reloadClazzes();
		act = new ArrayList<Map<String,Object>>();
		com = new ArrayList<Map<String,Object>>();
		
		for(int foo=1;foo<17;foo++){
			act.add((Map<String, Object>) new HashMap<String,Object>().put("doubles", foo/2.54));
			com.add((Map<String, Object>) new HashMap<String,Object>().put("doubles", foo/2.54));
		}
	}*/
	
	@Ignore // TODO: use Mockito
	public void testComperatorCatchBlock(){
		//test the fail if the values(order,ignorecase,key) arent setted
		MapComparator mock_jds = EasyMock.createNiceMock(MapComparator.class);
		MapComparator jdsort = new MapComparator();
		EasyMock.expect(  mock_jds.compare("invalid", "invalid")).andReturn(0);
		EasyMock.replay(mock_jds);
		assertEquals(jdsort.compare("invalid", "invalid") ,          0);
	}
	
	@Ignore // TODO: use Mockito
	public void testComperatorWithNr(){
		//dynamic nr-tests with both orders ASC and DESC
		//reloadClazzes();
		List<Map<String,Object>> nract = new ArrayList<Map<String,Object>>();
		List<Map<String,Object>> nrcom= new ArrayList<Map<String,Object>>();
		
		for(int i = 0;i<10;i++){
			Map<String,Object> obact = new HashMap<String,Object>();
			Map<String,Object> obcom = new HashMap<String,Object>();
			obact.put("ints", i);
			obcom.put("ints", i);
			
			nrcom.add(obcom);
			nract.add(obact);
		}
		
		for(int asc=0;asc<=1;asc++){
			for(int iact=0;iact<10;iact++){
				for(int icom=0;icom<10;icom++){
					int actual = (Integer) nract.get(0).get("ints");
					int compar = (Integer) nrcom.get(icom).get("ints");
					int expectNR = calcExpectedReturn(asc, actual, compar);
					
					MapComparator mock_jds = EasyMock.createNiceMock(MapComparator.class);
					
					EasyMock.expect(mock_jds.compare(nract.get(iact), nrcom.get(icom))).andReturn(expectNR);
					EasyMock.replay(mock_jds);
				}
			}
		}
	}
	
	private int calcExpectedReturn(int order, int act, int comp){
		if(order == 0)
			return Integer.compare(act, comp);
		return Integer.compare(comp, act);
	}
}