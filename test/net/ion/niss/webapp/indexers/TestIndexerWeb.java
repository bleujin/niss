package net.ion.niss.webapp.indexers;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.Searcher;
import net.ion.radon.client.StubServer;

public class TestIndexerWeb extends TestBaseIndexWeb {

	public void testIndexGet() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/index").get() ;

		Debug.line(response.contentsString());
	}
	
	
	public void testJsonUpdate() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/index.json").postParam("documents", "{id:'json', name:'bleujin', age:20}").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("1 indexed", response.contentsString());
		
		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		searcher.createRequest("id:json").find().debugPrint();
	}
	
	public void testJarrayUpdate() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/index.jarray").postParam("documents", "[{id:'jarray1', name:'jarray', age:20}, {id:'jarray2', name:'jarray', age:30}]").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("2 indexed", response.contentsString());

		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		searcher.createRequest("name:jarray").find().debugPrint();
	}
	
	public void testCsvUpdate() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/index.csv").postParam("documents", "id,name,age,title\ncsv1,csv,20,new\ncsv2,csv,30,new").postParam("boost", "2.0").postParam("overwrite", "true").post() ;
		assertEquals("2 indexed", response.contentsString());
		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		searcher.createRequest("name:csv").find().debugPrint();
		
	}
}
