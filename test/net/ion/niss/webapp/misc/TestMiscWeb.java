package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestMiscWeb extends TestCase {

	private StubServer ss;
	private REntry entry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(MiscWeb.class) ;
		this.entry = ss.treeContext().putAttribute(REntry.EntryName, REntry.testStup()) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		entry.close(); 
		super.tearDown();
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
	

	public void testEditProfile() throws Exception {
		StubHttpResponse response = ss.request("/misc/profile/bleujin@i-on.net").postParam("langcode", "kr").post() ;
		
		ReadSession session = entry.login() ;
		assertEquals("kr", session.pathBy("/users/bleujin@i-on.net").property("langcode").asString()) ;
		
	}
	
	public void testRemoveUser() throws Exception {
		StubHttpResponse response = ss.request("/misc/users/bleujin@i-on.net").delete() ;
		
		assertEquals("removed bleujin@i-on.net", response.contentsString());
	}
}
