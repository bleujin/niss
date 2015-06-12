package net.ion.niss.webapp.indexers;

import junit.framework.TestCase;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;

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
		Central c = CentralConfig.newRam().build() ;
		Indexer indexer = c.newIndexer() ;
		
		indexer.index(new IndexJob<Void>() {
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
