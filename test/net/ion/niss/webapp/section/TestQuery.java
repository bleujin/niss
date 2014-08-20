package net.ion.niss.webapp.section;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.MenuWeb;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestQuery extends TestCase {

	private StubServer ss;
	private REntry rentry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(SectionWeb.class, MenuWeb.class, TemplateWeb.class) ;
		this.rentry = REntry.test();
		ss.treeContext().putAttribute(REntry.EntryName, rentry) ;

		StubHttpResponse response = ss.request("/sections/sec1").post() ;
		assertEquals("created sec1", response.contentsString());
	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}
	
	public void testQuery() throws Exception {
		rentry.searchManager().searcher("sec1").search("").debugPrint();
	}
	
	public void testJson() throws Exception {
		StubHttpResponse response = ss.request("/sections/sec1/query.json?query=").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString());
		assertEquals(1, json.asJsonArray("docs").size());
	}
	
	
}
