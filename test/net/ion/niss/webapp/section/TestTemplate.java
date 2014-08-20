package net.ion.niss.webapp.section;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.MenuWeb;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.radon.client.StubServer;

public class TestTemplate extends TestCase {

	private StubServer ss;
	private REntry rentry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(SectionWeb.class, MenuWeb.class) ;
		this.rentry = REntry.test();
		ss.treeContext().putAttribute(REntry.EntryName, rentry) ;

		if (! rentry.searchManager().hasSearch("sec1")) {
			StubHttpResponse response = ss.request("/sections/sec1").post() ;
			assertEquals("created sec1", response.contentsString());
		}
	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}
	
	
	public void testViewTemplate() throws Exception {
		ss.request("/sections/sec1/template").postParam("template", "HelloWorld").post() ;
		StubHttpResponse response = ss.request("/sections/sec1/template").get() ;
		assertEquals("HelloWorld", response.contentsString());
	}
	
	
	public void testTemplateQuery() throws Exception {
		String template = IOUtil.toStringWithClose(SectionWeb.class.getResourceAsStream("default.template"));
		
		ss.request("/sections/sec1/template").postParam("template", template).post() ;
		
		StubHttpResponse response = ss.request("/sections/sec1/query.template?query=*%3A*").get() ;
		Debug.line(response.contentsString());
	}
}
