package net.ion.nsearcher.index.crawl;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.util.ListUtil;
import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Spider;
import net.ion.icrawler.Task;
import net.ion.icrawler.pipeline.Pipeline;
import net.ion.nsearcher.common.FieldIndexingStrategy;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.index.rdb.RDBIndexHandler;

public class CrawlIndexer {

	private Central central ;
	private Spider spider;
	private ExecutorService es ;
	private FieldIndexingStrategy fieldIndexStrategy;

	public CrawlIndexer(Central central, Spider spider, ExecutorService es, FieldIndexingStrategy fieldIndexStrategy) {
		this.central = central ;
		this.spider = spider ;
		this.es = es ;
		this.fieldIndexStrategy = fieldIndexStrategy ;
	}
	
	public <T> Future<T> index(CrawlIndexHandler crawlIndexHandler) {
		return index(central.indexConfig().indexAnalyzer(), crawlIndexHandler) ;
	}

	public <T> Future<List<T>> index(final Analyzer analyzer, final CrawlIndexHandler<T> crawlIndexHandler) {
		return es.submit(new Callable<List<T>>(){

			@Override
			public List<T> call() throws Exception {
				final Indexer indexer = central.newIndexer() ;
				
				return indexer.index(analyzer, new IndexJob<List<T>>() {
					@Override
					public List<T> handle(final IndexSession isession) throws Exception {
						isession.fieldIndexingStrategy(fieldIndexStrategy) ;
						final List<T> result = ListUtil.newList() ;
						spider.addPipeline(new Pipeline(){
							@Override
							public void process(ResultItems ritems, Task task) {
								try {
									result.add(crawlIndexHandler.onSuccess(isession, ritems, task)) ;
								} catch (IOException e) {
									result.add(crawlIndexHandler.onFail(isession, ritems, task, e)) ;
								}
							}
						}) ;
						
						spider.run(); 
						
						return result;
					}
					
				});
			}
		}) ;
	}


//	public <T> Future<T> index(final Analyzer analyzer, final RDBIndexHandler<T> rdbIndexHandler) {
//		return es.submit(new Callable<T>() {
//			@Override
//			public T call() throws Exception {
//				Indexer indexer = central.newIndexer();
//				return indexer.index(analyzer, new IndexJob<T>() {
//					@Override
//					public T handle(final IndexSession isession) throws Exception {
//						return rdb.handle(new ResultSetHandler<T>() {
//							@Override
//							public T handle(ResultSet rs) throws SQLException {
//								try {
//									return rdbIndexHandler.onSuccess(isession, rdb, rs);
//								} catch (IOException ex) {
//									return rdbIndexHandler.onFail(isession, rdb, ex) ;
//								}
//							}
//						});
//					}
//				});
//			}
//		});
//	}


}
