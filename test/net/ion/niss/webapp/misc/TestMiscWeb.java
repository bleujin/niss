package net.ion.niss.webapp.misc;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;
import junit.framework.TestCase;

public class TestMiscWeb extends TestCase {

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(MiscWeb.class) ;
		ss.treeContext().putAttribute(REntry.EntryName, REntry.test()) ;
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
	
	
	public void testUserList() throws Exception {
		StubHttpResponse response = ss.request("/misc/users").get() ;
		assertEquals(200, response.status());
		
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals(true, json.has("info"));
		assertEquals(true, json.has("schemaName"));
		assertEquals(true, json.has("users"));
	}
	
	public void testAddUser() throws Exception {
		StubHttpResponse response = ss.request("/misc/users/bleujin@i-on.net").postParam("name", "bleujin").post() ;
		
		assertEquals("registered bleujin@i-on.net", response.contentsString());
	}
	

	public void testRemoveUser() throws Exception {
		StubHttpResponse response = ss.request("/misc/users/bleujin@i-on.net").delete() ;
		
		assertEquals("removed bleujin@i-on.net", response.contentsString());
	}
}
