package net.ion.nsearcher.index.crawl;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.lucene.analysis.Analyzer;

import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.common.FieldIndexingStrategy;
import net.bleujin.searcher.index.IndexJob;
import net.bleujin.searcher.index.IndexSession;
import net.ion.framework.util.ListUtil;
import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Spider;
import net.ion.icrawler.Task;
import net.ion.icrawler.pipeline.Pipeline;

public class CrawlIndexer {

	private SearchController central ;
	private Spider spider;
	private ExecutorService es ;
	private FieldIndexingStrategy fieldIndexStrategy;

	public CrawlIndexer(SearchController central, Spider spider, ExecutorService es, FieldIndexingStrategy fieldIndexStrategy) {
		this.central = central ;
		this.spider = spider ;
		this.es = es ;
		this.fieldIndexStrategy = fieldIndexStrategy ;
	}
	
	public <T> Future<T> index(CrawlIndexHandler crawlIndexHandler) {
		return index(central.defaultIndexConfig().perFieldAnalyzer(), crawlIndexHandler) ;
	}

	public <T> Future<List<T>> index(final Analyzer analyzer, final CrawlIndexHandler<T> crawlIndexHandler) {
		return es.submit(new Callable<List<T>>(){

			@Override
			public List<T> call() throws Exception {
				return central.index(new IndexJob<List<T>>() {
					@Override
					public List<T> handle(final IndexSession isession) throws Exception {
						isession.indexConfig().indexAnalyzer(analyzer) ;
						
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
