package net.ion.niss.webapp.common;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.framework.util.Debug;

public class TestTrans  extends TestCase {

	private Craken r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testFirst() throws Exception {
		ReadSession session = r.login("test") ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").refTo("parent", "/emp") ;
				wsession.pathBy("/emps/hero").property("name", "hero").refTo("parent", "/emp") ;
				
				wsession.pathBy("/emps/bleujin/address").property("city", "seoul") ;
				return null;
			}
		}) ;
		
		
		Debug.line(session.pathBy("/emps").transformer(Trans.DECENT)) ; 
	}
	
}
