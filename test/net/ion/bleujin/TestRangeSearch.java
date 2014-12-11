package net.ion.bleujin;

import junit.framework.TestCase;
import net.ion.nsearcher.common.FieldIndexingStrategy;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

public class TestRangeSearch extends TestCase{
	private Central cen = null ; 
	public void setUp() throws Exception {
		super.setUp() ;
		cen = CentralConfig.newRam().build() ;
	}

	@Override
	protected void tearDown() throws Exception {
		cen.close();
		super.tearDown();
	}
	public void tegstRange() throws Exception {
		cen.newIndexer().index(new IndexJob<Void>() {
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
