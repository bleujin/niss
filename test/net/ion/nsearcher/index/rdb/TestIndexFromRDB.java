package net.ion.nsearcher.index.rdb;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.SearchControllerConfig;
import net.bleujin.searcher.index.IndexSession;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.niss.webapp.loaders.RDB;

public class TestIndexFromRDB extends TestCase {

	
	public void testFromRDB() throws Exception {
		SearchController cen = SearchControllerConfig.newRam().build() ;
		
		RDBIndexer rindexer = RDBIndexBuilder.create(cen)
			.rdb(RDB.oracle("61.250.201.239:1521:qm10g", "bleujin", "redf").query("select * from tabs"))
			.executors(new WithinThreadExecutor()) //
			.build() ;
		
		Future<Integer> future = rindexer.index(new RDBIndexHandler<Integer>() {
			@Override
			public Integer onSuccess(IndexSession isession, RDB rdb, ResultSet rs) throws IOException, SQLException {
				int i = 0 ;
				while(rs.next()){
					isession.newDocument(rs.getString("table_name")).unknown("owner", "bleujin").updateVoid() ;
					if (i++ % 2999 == 0) isession.continueUnit() ;
				}
				return i;
			}

			@Override
			public Integer onFail(IndexSession isession, RDB rdb, Exception ex) {
				return 0;
			}
		});
		
		cen.newSearcher().search("").debugPrint(); 

	}
}
