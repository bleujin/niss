package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

import net.bleujin.rcraken.ReadSession;
import net.bleujin.searcher.Searcher;
import net.bleujin.searcher.index.IndexSession;
import net.ion.framework.util.Debug;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.icrawler.Page;
import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Site;
import net.ion.icrawler.Spider;
import net.ion.icrawler.Task;
import net.ion.icrawler.pipeline.Pipeline;
import net.ion.icrawler.processor.PageProcessor;
import net.ion.icrawler.scheduler.MaxLimitScheduler;
import net.ion.icrawler.scheduler.QueueScheduler;
import net.ion.icrawler.selector.PlainLink;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.TestBaseIndexWeb;
import net.ion.nsearcher.index.crawl.CrawlIndexBuilder;
import net.ion.nsearcher.index.crawl.CrawlIndexHandler;
import net.ion.nsearcher.index.crawl.CrawlIndexer;

public class TestSampleScript3 extends TestBaseIndexWeb {

	private REntry rentry;
	private IndexManager imanager;
	private ReadSession rsession;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		
		this.imanager = rentry.indexManager();
		this.rsession = rentry.login() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		rentry.close();
		super.tearDown();
	}


	public void testFromCrawl() throws Exception {
		final String iid = "col1" ;
		String hostPattern = "http://www.i-on.net/*";
		final String urlPattern = (new StringBuilder("(")).append(hostPattern.replace(".", "\\.").replace("*", "[^\"'#]*")).append(")").toString();
		PageProcessor processor = new PageProcessor() {
			@Override
			public void process(Page page) {
				PlainLink links = page.getHtml().links().regex(urlPattern) ;
				page.putField("url", page.getRequest().getUrl()) ;
				page.putField("title", page.getHtml().xpath("//title/text()").get()) ;
				page.putField("links", page.getHtml().links().regex(urlPattern).targets()) ;
				
				page.addTargets(links.targets());
			}
		};
		
		Spider spider = Site.create("http://www.i-on.net/index.html").sleepTime(50).newSpider(processor).scheduler(new MaxLimitScheduler(new QueueScheduler(), 10))
			.addPipeline(new Pipeline() {
				@Override
				public void process(ResultItems ritems, Task task) {
					Debug.debug(ritems.getRequest().getUrl() + " processed");
				}
			}) ;

		
		CrawlIndexer cindexer = CrawlIndexBuilder.create(imanager.index(iid))
			.spider(spider)
			.executors(new WithinThreadExecutor())
			.build(imanager.fieldIndexStrategy(rsession, iid)) ;

		Future<List<Boolean>> future = cindexer.index(new CrawlIndexHandler<Boolean>(){
			public Boolean onSuccess(IndexSession isession, ResultItems ritems, Task task) throws IOException{
				Debug.line(ritems.asString("title"));
				isession.newDocument(ritems.getRequest().getUrl()).keyword("url", ritems.getRequest().getUrl()).text("title", ritems.asString("title")).updateVoid() ;
				return true ;
			}

			@Override
			public Boolean onFail(IndexSession isession, ResultItems ritems, Task task, IOException e) {
				return false;
			}
		}) ;
		
		Searcher searcher = imanager.index(iid).newSearcher() ;
		searcher.createRequest("").find().debugPrint(); 
		Debug.line(future.get());
	}
	
}
