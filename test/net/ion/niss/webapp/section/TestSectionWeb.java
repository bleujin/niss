package net.ion.niss.webapp.section;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.MenuWeb;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.section.SectionWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestSectionWeb extends TestCase {

	
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
	
	public void testListSection() throws Exception {
		
		StubHttpResponse response = ss.request("/sections").get() ;
		assertEquals("sec1",  new JsonParser().parse(response.contentsString()).getAsJsonArray().get(0).getAsJsonObject().asString("sid"));
	}
	
	public void testEditSection() throws Exception {
		StubHttpResponse response = ss.request("/sections/sec1/define")
			.postParam("target_collection", "document,abcd")
			.postParam("filter", "function()").postParam("applyfilter", "false").postParam("sort", "").postParam("applysort", "false")
			.postParam("handler", "function()").postParam("applyhandler", "false")
			.post() ;
		
		assertEquals("modified sec1", response.contentsString());
		
		
		response = ss.request("/sections/sec1/define").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		
		Debug.line(json);
		assertEquals("document", json.asJsonArray("collection").toObjectArray()[0]) ;
		assertEquals("abcd", json.asJsonArray("collection").toObjectArray()[1]) ;
		assertEquals("function()", json.asString("filter")) ;
		assertEquals("false", json.asString("applyfilter")) ;
		assertEquals("", json.asString("sort")) ;
		assertEquals("false", json.asString("applysort")) ;
		assertEquals("function()", json.asString("handler")) ;
		assertEquals("false", json.asString("applyhandler")) ;
	}
	
	
}
