package net.ion.niss.webapp.collection;

import junit.framework.TestCase;
import net.ion.niss.apps.old.IndexManager;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.Searcher;
import net.ion.radon.client.StubServer;

public class TestIndex extends TestCase {

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
	
	public void testJsonUpdate() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/index.json").postParam("documents", "{id:'json', name:'bleujin', age:20}").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("1 indexed", response.contentsString());
		
		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		searcher.createRequest("id:json").find().debugPrint();
	}
	
	public void testJarrayUpdate() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/index.jarray").postParam("documents", "[{id:'jarray1', name:'jarray', age:20}, {id:'jarray2', name:'jarray', age:30}]").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("2 indexed", response.contentsString());

		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		searcher.createRequest("name:jarray").find().debugPrint();
	}
	
	public void testCsvUpdate() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/index.csv").postParam("documents", "id,name,age,title\ncsv1,csv,20,new\ncsv2,csv,30,new").postParam("boost", "2.0").postParam("overwrite", "true").post() ;
		assertEquals("2 indexed", response.contentsString());
		REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		Searcher searcher = rentry.indexManager().index("col1").newSearcher() ;
		searcher.createRequest("name:csv").find().debugPrint();
		
	}
}
