package net.ion.nsearcher.index.crawl;

import java.util.concurrent.ExecutorService;

import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.common.FieldIndexingStrategy;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.icrawler.Spider;


public class CrawlIndexBuilder {

	private SearchController central;
	private Spider spider;
	private ExecutorService executors = new WithinThreadExecutor();

	public CrawlIndexBuilder(SearchController central) {
		this.central = central;
	}

	public static CrawlIndexBuilder create(SearchController central) {
		return new CrawlIndexBuilder(central);
	}

	public CrawlIndexBuilder spider(Spider spider){
		this.spider = spider ;
		return this ;
	}
	
	public Spider spider() {
		return spider;
	}
	
	public CrawlIndexer build(FieldIndexingStrategy fieldIndexStrategy) {
		return new CrawlIndexer(central, spider, executors, fieldIndexStrategy);
	}

	public CrawlIndexBuilder executors(ExecutorService es) {
		this.executors = es ;
		return this;
	}
	
}
