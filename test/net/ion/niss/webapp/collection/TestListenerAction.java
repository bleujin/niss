package net.ion.niss.webapp.collection;

import junit.framework.TestCase;
import net.ion.niss.apps.IdString;
import net.ion.niss.apps.collection.IndexManager;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.radon.client.StubServer;

public class TestListenerAction extends TestCase {

	private StubServer ss;
	private REntry entry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(CollectionWeb.class);
		this.entry = REntry.test();
		ss.treeContext().putAttribute(REntry.EntryName, entry);

		StubHttpResponse response = ss.request("/collections").postParam("cid", "col1").post();
		assertEquals("created col1", response.contentsString());
	}

	@Override
	protected void tearDown() throws Exception {
		ss.shutdown();
		super.tearDown();
	}
	
	
	public void testListenerAction() throws Exception {
		IndexManager im = entry.indexManager() ;
		IdString cid = IdString.create("col1");
		assertEquals(true, im.hasIndex(cid)) ;
		
		assertEquals(MyKoreanAnalyzer.class, im.index(cid).newIndexer().analyzer().getClass()) ;
	}
	
	public void testRemoveIndex() throws Exception {
		StubHttpResponse response = ss.request("/collections").postParam("cid", "col1").postParam("action", "remove").post();
		
		IndexManager im = entry.indexManager() ;
		IdString cid = IdString.create("col1");
		assertEquals(false, im.hasIndex(cid)) ;
	}
	
}
