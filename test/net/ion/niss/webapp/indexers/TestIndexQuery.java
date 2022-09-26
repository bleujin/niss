package net.ion.niss.webapp.indexers;

import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.Searcher;
import net.bleujin.searcher.index.IndexJob;
import net.bleujin.searcher.index.IndexSession;

public class TestIndexQuery extends TestBaseIndexWeb {
	
	public void testSort() throws Exception {
		SearchController central = entry.indexManager().index("col1") ;
		
		central.index(new IndexJob<Void>() {

			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.deleteAll() ;
				isession.newDocument("bleujin").keyword("name", "bleujin").number("age", 20).text("explain", "hello bleujin").update() ;
				isession.newDocument("hero").keyword("name", "hero").number("age", 30).text("explain", "hi hero").update() ;
				isession.newDocument("jin").keyword("name", "jin").number("age", 7).text("explain", "namaste jin").update() ;
				return null;
			}
		}) ;
		
		Searcher searcher = central.newSearcher() ;
		
		searcher.createRequest("").sort("age").find().debugPrint(); 
		
	}

}
