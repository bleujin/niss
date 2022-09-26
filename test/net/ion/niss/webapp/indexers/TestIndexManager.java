package net.ion.niss.webapp.indexers;

import junit.framework.TestCase;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.SearchControllerConfig;
import net.bleujin.searcher.index.IndexJob;
import net.bleujin.searcher.index.IndexSession;

public class TestIndexManager extends TestCase {

//	private StubServer ss;
//	private REntry entry;
//
//	@Override
//	protected void setUp() throws Exception {
//		super.setUp();
//
//		this.ss = StubServer.create(IndexerWeb.class);
//		this.entry = REntry.test();
//		ss.treeContext().putAttribute(REntry.EntryName, entry);
//
//		if (! entry.indexManager().hasIndex("col1")){
//			StubHttpResponse response = ss.request("/indexers/col1").postParam("cid", "col1").post();
//			assertEquals("created col1", response.contentsString());
//		}
//	}
//
//	@Override
//	protected void tearDown() throws Exception {
//		ss.shutdown();
//		super.tearDown();
//	}
	
	
	
	
	
	
	
	public void testKeyword() throws Exception {
		SearchController c = SearchControllerConfig.newRam().build() ;
		
		c.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.newDocument().unknown("name", "0708").insert() ;
				return null;
			}
		}) ;

		// c.newSearcher().search("name:0708").debugPrint(); 

		c.newSearcher().search("0708").debugPrint("name"); 
		c.close(); 
	}
}
