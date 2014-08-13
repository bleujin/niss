package net.ion.niss.webapp.misc;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;
import junit.framework.TestCase;

public class TestMiscWeb extends TestCase {

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(MiscWeb.class) ;
	}
	
	public void testThread() throws Exception {
		
		StubHttpResponse response = ss.request("/misc/thread").get() ;
		assertEquals(200, response.status());
		assertEquals(true, JsonObject.fromString(response.contentsString()).has("threadDump"));
	}
	
	public void testProperties() throws Exception {
		StubHttpResponse response = ss.request("/misc/properties").get() ;
		assertEquals(200, response.status());
		assertEquals(true, JsonObject.fromString(response.contentsString()).keySet().size() > 1);
	}
	
}
