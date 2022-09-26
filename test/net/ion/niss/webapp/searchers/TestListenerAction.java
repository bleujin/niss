package net.ion.niss.webapp.searchers;

import net.bleujin.searcher.Searcher;
import net.ion.nradon.stub.StubHttpResponse;

public class TestListenerAction extends TestBaseSearcher {


	public void testSectionSearchManager() throws Exception {
		assertEquals(true, rentry.indexManager().hasIndex("document")) ;
		
		assertEquals(true, rentry.searchManager().hasSearch("sec1")) ;
		Searcher searcher = rentry.searchManager().searcher("sec1") ;
		
//		assertEquals(2, searcher.readerCount());
	}
	
	public void testSearchAll() throws Exception {
		rentry.searchManager().searcher("sec1").search("").debugPrint(); 
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
		
//		CompositeSearcher searcher = (CompositeSearcher) rentry.searchManager().searcher("sec1") ;
//		assertEquals(1, searcher.readerCount());
	}

}
