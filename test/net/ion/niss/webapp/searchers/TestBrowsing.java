package net.ion.niss.webapp.searchers;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestBrowsing extends TestCase{

	private StubServer ss;
	private REntry rentry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(SearcherWeb.class) ;
		this.rentry = REntry.create();
		ss.treeContext().putAttribute(REntry.EntryName, rentry) ;

		StubHttpResponse response = ss.request("/searchers/sec1").post() ;
		assertEquals("created sec1", response.contentsString());
	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}
	
	
	public void testBrowsing() throws Exception {
		StubHttpResponse response = ss.request("/searchers/sec1/browsing").get() ;
		
		Debug.line(response.contentsString()) ;
	}
	
}
