package net.ion.niss.webapp.section;

import junit.framework.TestCase;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.CompositeSearcher;
import net.ion.radon.client.StubServer;

public class TestListenerAction extends TestCase {

	private StubServer ss;
	private REntry entry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(SectionWeb.class);
		this.entry = REntry.test();
		ss.treeContext().putAttribute(REntry.EntryName, entry);
	}
	public void testSectionSearchManager() throws Exception {
		assertEquals(true, entry.indexManager().hasIndex("document")) ;
		
		assertEquals(true, entry.searchManager().hasSearch("sec1")) ;
		CompositeSearcher searcher = (CompositeSearcher) entry.searchManager().searcher("sec1") ;
		
		assertEquals(2, searcher.readerCount());
	}
	
	public void testSearchAll() throws Exception {
		entry.searchManager().searcher("sec1").search("").debugPrint(); 
	}
	
	public void testCreateNewSection() throws Exception {
//		@POST
//		@Path("/{sid}/define")
//		public String defineSection(@PathParam("sid") final String sid, @FormParam("target_collection") final String collections
//					, @Context HttpRequest request
//					, @FormParam("filter") final String filter, @DefaultValue("false") @FormParam("applyfilter") final boolean applyFilter
//					, @FormParam("sort") final String sort, @DefaultValue("false") @FormParam("applysort") final boolean applySort
//					, @FormParam("handler") final String handler, @DefaultValue("false") @FormParam("applyhandler") final boolean applyHandler) {
		StubHttpResponse response = ss.request("/sections/sec1/define").postParam("target_collection", "document").post() ;
		
		CompositeSearcher searcher = (CompositeSearcher) entry.searchManager().searcher("sec1") ;
		assertEquals(1, searcher.readerCount());
	}

}
