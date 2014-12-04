package net.ion.niss.webapp.common;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.indexers.TestIndexQuery;
import junit.framework.TestCase;

public class TestTrans  extends TestCase {

	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
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
