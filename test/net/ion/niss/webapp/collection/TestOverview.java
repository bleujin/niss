package net.ion.niss.webapp.collection;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.niss.apps.IdString;
import net.ion.niss.apps.collection.IndexManager;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.radon.client.StubServer;
import net.ion.radon.core.let.PathHandler;

public class TestOverview extends TestCase  {
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
	

	public void testListCollection() throws Exception {
		StubHttpResponse response = ss.request("/collections").get();
		assertEquals("col1", new JsonParser().parse(response.contentsString()).getAsJsonArray().get(0).getAsJsonObject().asString("cid"));
	}
	
	public void testStatus() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/overview").get() ;
		
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals(true, json.has("info")) ;
		assertEquals(true, json.has("status")) ;
		assertEquals(true, json.has("dirInfo")) ;
		
		Debug.line(response.contentsString());
	}
	
	
	public void testChangeAnalyzer() throws Exception {
		ss.request("/collections/col1/fields").postParam("field", "indexanalyzer").postParam("content", StandardAnalyzer.class.getCanonicalName()).post();
		ss.request("/collections/col1/fields").postParam("field", "applystopword").postParam("content", "true").post();

		StubHttpResponse response = ss.request("/collections/col1/overview").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals(true, json.has("indexanalyzer")) ;
		assertEquals(StandardAnalyzer.class.getCanonicalName(), json.asString("indexanalyzer")) ;
		assertEquals(true, json.has("applystopword")) ;
		assertEquals("true", json.asString("applystopword")) ;
		
		Debug.line(json.asJsonArray("analyzer")) ;
	}
	
	public void testApplyStopword() throws Exception {
		
	}
	
	
	public void xtestServer() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(9500).add(new PathHandler(OldCollectionWeb.class).prefixURI("/admin")).start().get() ;
		new InfinityThread().startNJoin(); 
	}
}
