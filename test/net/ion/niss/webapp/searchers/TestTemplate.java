package net.ion.niss.webapp.searchers;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.misc.MenuWeb;
import net.ion.niss.webapp.searchers.SearcherWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.radon.client.StubServer;

public class TestTemplate extends TestCase {

	private StubServer ss;
	private REntry rentry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(SearcherWeb.class, MenuWeb.class) ;
		this.rentry = REntry.test();
		ss.treeContext().putAttribute(REntry.EntryName, rentry) ;

		if (! rentry.searchManager().hasSearch("sec1")) {
			StubHttpResponse response = ss.request("/searchers/sec1").post() ;
			assertEquals("created sec1", response.contentsString());
		}
	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}
	
	
	public void testViewTemplate() throws Exception {
		ss.request("/searchers/sec1/template").postParam("template", "HelloWorld").post() ;
		StubHttpResponse response = ss.request("/searchers/sec1/template").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals("HelloWorld", json.asString("template"));
	}
	
	
	public void testTemplateQuery() throws Exception {
		String template = IOUtil.toStringWithClose(SearcherWeb.class.getResourceAsStream("default.template"));
		
		ss.request("/searchers/sec1/template").postParam("template", template).post() ;
		
		StubHttpResponse response = ss.request("/searchers/sec1/query.template?query=*%3A*").get() ;
		Debug.line(response.contentsString());
	}
}