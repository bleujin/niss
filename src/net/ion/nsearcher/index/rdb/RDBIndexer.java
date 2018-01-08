package net.ion.nsearcher.index.rdb;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.lucene.analysis.Analyzer;

import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.niss.webapp.loaders.RDB;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;

public class RDBIndexer {

	private Central central;
	private RDB rdb;
	private ExecutorService es;

	public RDBIndexer(Central central, RDB rdb, ExecutorService es) {
		this.central = central;
		this.rdb = rdb;
		this.es = es;
	}

	public <T> Future<T> index(final RDBIndexHandler<T> rdbIndexHandler) {
		return index(central.indexConfig().indexAnalyzer(), rdbIndexHandler) ;
	}
	
	public <T> Future<T> index(final Analyzer analyzer, final RDBIndexHandler<T> rdbIndexHandler) {
		return es.submit(new Callable<T>() {
			@Override
			public T call() throws Exception {
				Indexer indexer = central.newIndexer();
				return indexer.index(analyzer, new IndexJob<T>() {
					@Override
					public T handle(final IndexSession isession) throws Exception {
						return rdb.handle(new ResultSetHandler<T>() {
							@Override
							public T handle(ResultSet rs) throws SQLException {
								try {
									return rdbIndexHandler.onSuccess(isession, rdb, rs);
								} catch (IOException ex) {
									return rdbIndexHandler.onFail(isession, rdb, ex) ;
								}
							}
						});
					}
				});
			}
		});
	}

}
