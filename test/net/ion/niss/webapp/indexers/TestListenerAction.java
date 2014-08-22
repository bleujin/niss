package net.ion.niss.webapp.indexers;

import junit.framework.TestCase;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.IndexerWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.radon.client.StubServer;

public class TestListenerAction extends TestBaseIndexWeb {

	public void testListenerAction() throws Exception {
		IndexManager im = entry.indexManager() ;
		IdString cid = IdString.create("col1");
		assertEquals(true, im.hasIndex(cid)) ;
		
		assertEquals(MyKoreanAnalyzer.class, im.index(cid).newIndexer().analyzer().getClass()) ;
	}
	
	public void testRemoveIndex() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1").delete();
		
		IndexManager im = entry.indexManager() ;
		IdString cid = IdString.create("col1");
		assertEquals(false, im.hasIndex(cid)) ;
	}
	
}
