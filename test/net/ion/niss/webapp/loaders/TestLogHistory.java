package net.ion.niss.webapp.loaders;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestLogHistory extends TestCase {
	
	private StubServer ss;
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(LoaderWeb.class) ;
		ss.treeContext().putAttribute(REntry.EntryName, REntry.test()) ;
		ss.treeContext().putAttribute(EventSourceEntry.EntryName, EventSourceEntry.create()) ;
		ss.treeContext().putAttribute(JScriptEngine.EntryName, JScriptEngine.create()) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}

	
	public void testList() throws Exception {
		StubHttpResponse response = ss.request("/loaders/history").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString());
		
		assertEquals(true, json.has("info"));
		
		Debug.line(response.contentsString());
	}
	
}
