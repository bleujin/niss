package net.ion.niss.webapp.collection;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestSchema extends TestCase {
	private StubServer ss;
	private REntry entry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(CollectionWeb.class);
		this.entry = REntry.test();
		ss.treeContext().putAttribute(REntry.EntryName, entry);

		if (! entry.indexManager().hasIndex("col1")){
			StubHttpResponse response = ss.request("/collections").postParam("cid", "col1").post();
			assertEquals("created col1", response.contentsString());
		}
	}

	@Override
	protected void tearDown() throws Exception {
		ss.shutdown();
		super.tearDown();
	}
	
	
	public void testAddSchema() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/schema").postParam("fieldid", "name").postParam("ftype", "text").postParam("analyze", "true").post() ;
		assertEquals(true, response.contentsString().startsWith("created schema name")) ;
		
		ReadNode found = entry.login().pathBy("/collections/col1/schema/name") ;
		assertEquals(Boolean.TRUE, found.property("analyze").asBoolean());
	}
	
	public void testListSchema() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/schema").get() ;
		Debug.line(response.contentsString());
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		
		assertEquals("name", json.asJsonArray("fields").get(0).getAsJsonObject().asString("fieldid"));
	}
	
	public void testRemoveSchema() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/schema/name").delete() ;
		assertEquals(true, response.contentsString().startsWith("removed schema name")) ;
	}
	
	
}
