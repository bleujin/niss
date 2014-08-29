package net.ion.niss.webapp.indexers;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.Searcher;
import net.ion.radon.client.StubServer;

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
	
}
