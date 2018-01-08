package net.ion.niss.webapp.searchers;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.WriteNode;

public class TestSort extends TestCase {

	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = CrakenConfig.mapMemory().build() ;
		this.session = r.login("test") ;
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}
	
	public void testMakeSchemaInfo() throws Exception {
		session.tran(wsession -> {
			WriteNode wnode = wsession.pathBy("/searchers/sec1/schema") ;
			// wnode.child("num").property("schematype", ) // schematype, analyzer, analyze, store, boost
		}) ;
		
		
	}
	
}
