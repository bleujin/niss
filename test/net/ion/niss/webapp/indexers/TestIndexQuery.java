package net.ion.niss.webapp.indexers;

import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.Searcher;

public class TestIndexQuery extends TestBaseIndexWeb {
	
	public void testSort() throws Exception {
		Central central = entry.indexManager().index("col1") ;
		
		Indexer indexer = central.newIndexer() ;
		
		indexer.index(new IndexJob<Void>() {

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
