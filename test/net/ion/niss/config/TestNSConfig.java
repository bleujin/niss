package net.ion.niss.config;

import java.util.Set;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;

public class TestNSConfig extends TestCase {


	
	public void testLoad() throws Exception {
		Craken r = CrakenConfig.mapMemory().build().start() ;
		ReadSession session = r.login("search") ;
		
		
		ReadNode node = session.pathBy("/") ;
		Set<String> keys = node.keys() ;
		
		assertEquals(0, keys.size()) ;
		r.shutdown() ;
	}
	
}
