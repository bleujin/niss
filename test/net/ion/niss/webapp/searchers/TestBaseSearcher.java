package net.ion.niss.webapp.searchers;

import junit.framework.TestCase;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.misc.MenuWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestBaseSearcher extends TestCase{

	protected StubServer ss;
	protected REntry rentry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(SearcherWeb.class, MenuWeb.class, TemplateWeb.class) ;
		this.rentry = REntry.test();
		ss.treeContext().putAttribute(REntry.EntryName, rentry) ;

		StubHttpResponse response = ss.request("/searchers/sec1").post() ;
		assertEquals("created sec1", response.contentsString());
	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}
	
}
