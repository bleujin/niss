package net.ion.niss.webapp.loader;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.loader.LoaderWeb;
import net.ion.niss.webapp.section.SectionWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestLoaderWeb extends TestCase {
	
	private StubServer ss;


	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(LoaderWeb.class) ;
		ss.treeContext().putAttribute(REntry.EntryName, REntry.test()) ;

	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}

	public void testCreate() throws Exception {
		StubHttpResponse response = ss.request("/loaders").postParam("explain", "Sample From DB").postParam("content", "function()").post() ;
		assertEquals(true, response.contentsString().startsWith("created "));
		
		response = ss.request("/loaders").get() ;
		JsonObject json = JsonParser.fromString(response.contentsString()).getAsJsonArray().get(0).getAsJsonObject();
		assertEquals("Sample From DB", json.asString("explain"));
		assertEquals(true, json.has("lid"));
	}
	
	
	public void testRemove() throws Exception {
		StubHttpResponse response = ss.request("/loaders").postParam("explain", "Sample From DB").postParam("content", "function()").post() ;
		assertEquals(true, response.contentsString().startsWith("created "));
		
		
		String newId = StringUtil.substringAfter(response.contentsString(), "created ") ;
		response = ss.request("/loaders/" + newId).delete() ;
		
		assertEquals("deleted " + newId, response.contentsString());
	}
	
	
}
