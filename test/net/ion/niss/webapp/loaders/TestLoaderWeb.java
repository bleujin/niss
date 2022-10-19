package net.ion.niss.webapp.loaders;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestLoaderWeb extends TestCase {
	
	private StubServer ss;
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(LoaderWeb.class) ;
		ss.treeContext().putAttribute(REntry.EntryName, REntry.testStup()) ;
		ss.treeContext().putAttribute(EventSourceEntry.EntryName, EventSourceEntry.create()) ;
		ss.treeContext().putAttribute(JScriptEngine.EntryName, JScriptEngine.create()) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}

	public void testDefine() throws Exception {
		StubHttpResponse response = ss.request("/loaders/123/define").postParam("name", "Sample From DB").postParam("content", "function(){};").post() ;
		assertEquals(true, response.contentsString().startsWith("defined loader"));
		
		response = ss.request("/loaders").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString());
		
		Debug.line(json);
		assertEquals(true, json.has("loaders"));
	}
	
	
	public void testRemove() throws Exception {
		StubHttpResponse response = ss.request("/loaders/123/define").postParam("name", "Sample From DB").postParam("content", "function(){}").post() ;
		assertEquals(true, response.contentsString().startsWith("defined loader "));
		
		
		String newId = StringUtil.substringAfterLast(response.contentsString(), " ") ;
		response = ss.request("/loaders/" + newId).delete() ;
		
		assertEquals("deleted " + newId, response.contentsString());
	}
	
	
	
	public void testOverview() throws Exception {
		ss.request("/loaders/123/define").postParam("name", "Sample From DB").postParam("content", "function(){};").post() ;

		StubHttpResponse response = ss.request("/loaders/123/overview").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString());
		
		assertEquals(true, json.has("info"));
		
		Debug.line(response.contentsString());
	}
	
}
