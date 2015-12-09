package net.ion.niss.webapp.searchers;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;

public class TestSort extends TestCase {

	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest() ;
		this.session = r.login("test") ;
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testMakeSchemaInfo() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode wnode = wsession.pathBy("/searchers/sec1/schema") ;
				
				// wnode.child("num").property("schematype", ) // schematype, analyzer, analyze, store, boost
				
				return null;
			}
		}) ;
		
		
	}
	
}