package net.ion.niss.webapp.indexers;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import junit.framework.TestCase;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.Searcher;
import net.bleujin.searcher.common.ReadDocument;
import net.ion.framework.parse.gson.JsonArray;
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

		this.ss = StubServer.create(IndexerWeb.class);
		this.entry = REntry.testStup();
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
		assertTrue(found.property("analyze").asBoolean());
	}
	
	public void testListSchema() throws Exception {
		ss.request("/indexers/col1/schema").postParam("schemaid", "name").postParam("schematype", "text").postParam("analyze", "true").post() ;
		ss.request("/indexers/col1/schema").postParam("schemaid", "explain").postParam("schematype", "manual").postParam("analyze", "true").postParam("analyzer", SimpleAnalyzer.class.getCanonicalName()).post() ;
		
		StubHttpResponse response = ss.request("/indexers/col1/schema").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		
		JsonArray schemas = json.asJsonArray("data") ;
		for (int i = 0; i < schemas.size() ; i++) {
			JsonArray sinfo = schemas.asJsonArray(i) ;
			Debug.line(sinfo);
		}
	}
	
	public void testRemoveSchema() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/schema/name").delete() ;
		assertEquals(true, response.contentsString().startsWith("removed schema name")) ;
	}
	
	
	

	public void testApplySchema() throws Exception {
		ReadSession rsession = entry.login() ;
		rsession.pathBy("/indexers/col1/schema").children().debugPrint();
		
		rsession.tran(wsession -> {
			wsession.pathBy("/indexers/col1/schema/name").property("schematype", "manual").property("analyze", false).property("store", true).property("boost", 2.0D).merge();
			wsession.pathBy("/indexers/col1/schema/name2").property("schematype", "text").property("analyze", true).property("store", true).property("boost", 2.0D).merge();
		}) ;
		
		StubHttpResponse response = ss.request("/indexers/col1/index.json").postParam("documents", "{id:'schematest', name:'deview3', name2:'deview3'}").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("1 indexed", response.contentsString());
		
		Searcher searcher = entry.indexManager().index("col1").newSearcher() ;
		ReadDocument rdoc = searcher.search("name:'deview3'").first() ;

		Debug.line(rdoc.getField("name").fieldType()) ;
		Debug.line(rdoc.getField("name").fieldType().stored(), rdoc.getField("name").fieldType().indexOptions(), rdoc.getField("name").fieldType().tokenized()) ;
	}
	
	
	public void testManualSchema() throws Exception {
		IndexerWeb iw = new IndexerWeb(entry);
		String result = iw.addSchema("col1", "explain", "manual", WhitespaceAnalyzer.class.getCanonicalName(), true, false, "1.0") ;
		ReadSession rsession = entry.login() ;
		
		assertEquals(true, rsession.exist("/indexers/col1/schema/explain")) ;
		SearchController cen = entry.indexManager().index("col1") ;
		
		assertEquals(PerFieldAnalyzerWrapper.class.getCanonicalName(),  cen.defaultIndexConfig().analyzer().getClass().getCanonicalName());
		

		entry.reload(); 
		Debug.line(entry.indexManager().index("col1").defaultIndexConfig().analyzer()) ;

		iw.removeSchema("col1", "explain") ;
		cen = entry.indexManager().index("col1") ;
		assertEquals(StandardAnalyzer.class.getCanonicalName(), cen.defaultIndexConfig().analyzer().getClass().getCanonicalName());
	}
	
	
	
}
