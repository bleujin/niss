package net.ion.nsearcher.index.crawl;

import java.util.concurrent.ExecutorService;

import net.ion.icrawler.Spider;
import net.ion.nsearcher.common.FieldIndexingStrategy;
import net.ion.nsearcher.config.Central;

import org.infinispan.util.concurrent.WithinThreadExecutor;

public class CrawlIndexBuilder {

	private Central central;
	private Spider spider;
	private ExecutorService executors = new WithinThreadExecutor();

	public CrawlIndexBuilder(Central central) {
		this.central = central;
	}

	public static CrawlIndexBuilder create(Central central) {
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
