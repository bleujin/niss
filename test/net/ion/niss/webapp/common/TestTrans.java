package net.ion.niss.webapp.common;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.Debug;

public class TestTrans  extends TestCase {

	private Craken r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = CrakenConfig.mapMemory().build() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}
	
	public void testFirst() throws Exception {
		ReadSession session = r.login("test") ;
		
		session.tran(wsession -> {
			wsession.pathBy("/emps/bleujin").property("name", "bleujin").refTo("parent", "/emp").merge();
			wsession.pathBy("/emps/hero").property("name", "hero").refTo("parent", "/emp").merge() ;
			
			wsession.pathBy("/emps/bleujin/address").property("city", "seoul").merge() ;
		}) ;
		
		
		Debug.line(session.pathBy("/emps").transformer(Trans.DECENT)) ; 
	}
	
}
