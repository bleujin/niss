package net.ion.bleujin;

import junit.framework.TestCase;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.SearchControllerConfig;
import net.bleujin.searcher.index.IndexJob;
import net.bleujin.searcher.index.IndexSession;

public class TestRangeSearch extends TestCase{
	private SearchController cen = null ; 
	public void setUp() throws Exception {
		super.setUp() ;
		cen = SearchControllerConfig.newRam().build() ;
	}

	@Override
	protected void tearDown() throws Exception {
		cen.close();
		super.tearDown();
	}
	public void tegstRange() throws Exception {
		cen.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				for (int i = 0; i < 10; i++) {
					isession.newDocument(i * 3 + "").unknown("num", "" + i * 3).insert() ;
				}
				return null ;
			}
		}) ;
		
		cen.newSearcher().createRequest("num:[+2 TO +12]").find().debugPrint(); 
	}

}
