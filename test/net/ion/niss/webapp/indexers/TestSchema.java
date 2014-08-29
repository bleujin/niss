package net.ion.niss.webapp.indexers;

import org.apache.lucene.analysis.core.SimpleAnalyzer;

import Acme.Serve.SimpleAcceptor;
import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.indexers.IndexerWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.search.Searcher;
import net.ion.radon.client.StubServer;

public class TestSchema extends TestCase {
	private StubServer ss;
	private REntry entry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(IndexerWeb.class);
		this.entry = REntry.test();
		ss.treeContext().putAttribute(REntry.EntryName, entry);

		if (! entry.indexManager().hasIndex("col1")){
			StubHttpResponse response = ss.request("/indexers/col1").postParam("cid", "col1").post();
			assertEquals("created col1", response.contentsString());
		}
	}

	@Override
	protected void tearDown() throws Exception {
		ss.shutdown();
		super.tearDown();
	}
	
	
	public void testAddSchema() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/schema").postParam("schemaid", "name").postParam("schematype", "text").postParam("analyze", "true").post() ;
		assertEquals(true, response.contentsString().startsWith("created schema name")) ;
		
		ReadNode found = entry.login().pathBy("/indexers/col1/schema/name") ;
		assertEquals(Boolean.TRUE, found.property("analyze").asBoolean());
	}
	
	public void testListSchema() throws Exception {
		ss.request("/indexers/col1/schema").postParam("schemaid", "name").postParam("schematype", "text").postParam("analyze", "true").post() ;
		
		StubHttpResponse response = ss.request("/indexers/col1/schema").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		
		assertEquals("name", json.asJsonArray("schemas").get(0).getAsJsonObject().asString("schemaid"));
	}
	
	public void testRemoveSchema() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/schema/name").delete() ;
		assertEquals(true, response.contentsString().startsWith("removed schema name")) ;
	}
	
	
	

	public void testApplySchema() throws Exception {
		ReadSession rsession = entry.login() ;
		rsession.ghostBy("/indexers/col1/schema").children().debugPrint();
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				// .property("schematype", schematype).property("analyze", analyzer).property("store", store).property("boost", Double.valueOf(StringUtil.defaultIfEmpty(boost, "1.0"))) ;
				wsession.pathBy("/indexers/col1/schema/name").property("schematype", "manual").property("analyze", false).property("store", true).property("boost", 2.0D) ;
				wsession.pathBy("/indexers/col1/schema/name2").property("schematype", "text").property("analyze", true).property("store", true).property("boost", 2.0D) ;
				return null;
			}
		}) ;
		
		StubHttpResponse response = ss.request("/indexers/col1/index.json").postParam("documents", "{id:'schematest', name:'deview3', name2:'deview3'}").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("1 indexed", response.contentsString());
		
		Searcher searcher = entry.indexManager().index("col1").newSearcher() ;
		searcher.search("name:'deview3'").debugPrint();
		ReadDocument rdoc = searcher.createRequest("name:'deview3'", new SimpleAnalyzer(SearchConstant.LuceneVersion)).findOne() ;
		
		Debug.line(rdoc.getField("name").fieldType()) ;
		Debug.line(rdoc.getField("name").fieldType().stored(), rdoc.getField("name").fieldType().indexed(), rdoc.getField("name").fieldType().tokenized()) ;
	}
}
