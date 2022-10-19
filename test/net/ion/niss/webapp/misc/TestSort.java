package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.bleujin.rcraken.ReadSession;
import net.ion.niss.webapp.REntry;

public class TestSort extends TestCase {

	public void testSortLong() throws Exception {
		REntry re = REntry.testStup() ;
		
		ReadSession session = re.login() ;
		session.tran(wsession -> {
			for (int i = 0; i < 10; i++) {
				wsession.pathBy("/bleujin/" + i).property("dummy", 1L * i).merge();
			}
			return null ;
		}) ;
		
		session.pathBy("/bleujin").childQuery("").descending("dummy").find().debugPrint(); 
	}
}
