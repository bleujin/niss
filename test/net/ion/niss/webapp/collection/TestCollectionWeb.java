package net.ion.niss.webapp.collection;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.section.SectionWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestCollectionWeb extends TestCase {

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(CollectionWeb.class);
		ss.treeContext().putAttribute(REntry.EntryName, REntry.test());

		StubHttpResponse response = ss.request("/collections").postParam("cid", "col1").post();
		assertEquals("created col1", response.contentsString());
	}

	@Override
	protected void tearDown() throws Exception {
		ss.shutdown();
		super.tearDown();
	}

	public void testListCollection() throws Exception {
		StubHttpResponse response = ss.request("/collections").get();
		assertEquals("col1", new JsonParser().parse(response.contentsString()).getAsJsonArray().get(0).getAsJsonObject().asString("cid"));
	}
	
	
	public void testEditAnalyzer() throws Exception {
		
		
	}
	
	

}
