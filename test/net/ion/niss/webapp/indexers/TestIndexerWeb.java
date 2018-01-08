package net.ion.niss.webapp.indexers;

import java.util.List;

import net.bleujin.rcraken.ReadSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.Searcher;

public class TestIndexerWeb extends TestBaseIndexWeb {

	public void testIndexGet() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/index").get() ;
		assertEquals(true, JsonObject.fromString(response.contentsString()).has("info"));
		
	}
	
	public void testJsonUpdate() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/index.json").postParam("documents", "{id:'json', name:'bleujin', age:20}").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("1 indexed", response.contentsString());
		
		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		assertEquals(1, searcher.createRequest("id:json").find().size()) ;
	}
	
	public void testBoost() throws Exception {
		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		rentry.indexManager().index("col1").newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession indexsession) throws Exception {
				indexsession.deleteAll() ;
				return null;
			}
		}) ;
		
		ss.request("/indexers/col1/index.json").postParam("documents", "{id:'b2', name:'bleu jin', age:20}").postParam("boost", "2.0").postParam("overwrite", "true").post() ;
		ss.request("/indexers/col1/index.json").postParam("documents", "{id:'b1', name:'bleu jin', age:20}").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		ss.request("/indexers/col1/index.json").postParam("documents", "{id:'b3', name:'bleu jin', age:20}").postParam("boost", "3.0").postParam("overwrite", "true").post() ;
		ss.request("/indexers/col1/index.json").postParam("documents", "{id:'b4', name:'bleu jin', age:20}").postParam("boost", "4.0").postParam("overwrite", "true").post() ;

		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		SearchResponse response = searcher.createRequest("name:jin").find() ;

		List<ReadDocument> docs = response.getDocument() ;
		assertEquals("b4", docs.get(0).asString("id"));
		assertEquals("b3", docs.get(1).asString("id"));
		assertEquals("b2", docs.get(2).asString("id"));
		assertEquals("b1", docs.get(3).asString("id"));
		response.debugPrint(); 
	}
	
	
	
	
	public void testJarrayUpdate() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/index.jarray").postParam("documents", "[{id:'jarray1', name:'jarray', age:20}, {id:'jarray2', name:'jarray', age:30}]").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("2 indexed", response.contentsString());

		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		assertEquals(2, searcher.createRequest("name:jarray").find().size()) ;
	}
	
	public void testCsvUpdate() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/index.csv").postParam("documents", "id,name,age,title\ncsv1,csv,20,new\ncsv2,csv,30,new").postParam("boost", "2.0").postParam("overwrite", "true").post() ;
		assertEquals("2 indexed", response.contentsString());
		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		assertEquals(2, searcher.createRequest("name:csv").find().size()) ;
	}
	
	
	public void testApplySchema() throws Exception {
		
		final REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Central im = rentry.indexManager().index("col1");
		Indexer indexer = im.newIndexer() ;

		ReadSession session = rentry.login() ;
		session.tran(wsession -> {
			wsession.pathBy("/indexers/col1/schema/explain").property("schematype", "manual").property("analyze", true).property("store", true).property("boost", 2.0f).merge();
		}) ;
		
		
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.fieldIndexingStrategy(rentry.indexManager().fieldIndexStrategy(rentry.login(), "col1")) ;
				
				WriteDocument wdoc = isession.newDocument("test.doc").unknown(MapUtil.<String>chainKeyMap().put("name", "bleujin").put("age", "20").put("explain", "hello world").toMap()) ;
				Debug.line(wdoc.update().toLuceneDoc().getField("explain").boost()) ;
				return null;
			}
		}) ;
		
		Searcher searcher = im.newSearcher() ;
		
		ReadDocument doc = searcher.search("hello").first() ;
		assertEquals("hello world", doc.asString("explain"));
		Debug.line(doc.getField("explain").boost()) ;
	}

	
	
	
}
