package net.ion.niss.webapp.sites;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.Term;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import net.bleujin.rcraken.Fqn;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.StringUtil;
import net.ion.icrawler.Spider;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.SchemaInfos;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;

public class SiteManager {

	final Logger logger = LogBroker.getLogger(getClass()) ;
	private IndexManager indexManager;
	
	public SiteManager(IndexManager indexManager) {
		this.indexManager = indexManager ;
	}

	public Central index(IdString iid){
		return indexManager.index(iid) ;
	}	
	
	public Central index(String iid){
		return index(IdString.create(iid)) ;
	}

	public boolean hasIndex(String iid){
		return indexManager.hasIndex(iid) ;
	}

	public void crawlSite(final ReadSession rsession, final CrawlOption coption) throws Exception {

		new Thread(){
			public void run(){
				Spider spider = null ;
				try {
					SiteProcessor processor = SiteProcessor.create(rsession, coption.siteUrl(), coption);
					spider = processor.newSpider();
					spider.run(); // .setScheduler(new MaxLimitScheduler(new QueueScheduler(), 20))
				} catch (SQLException e) { 
					e.printStackTrace(); 
				} finally {
					if (spider != null) spider.close(); 
				}
			}
		}.start(); 
	}
	
	
	public void indexCrawlSite(final ReadSession rsession, final String siteId, final SchemaInfos sinfos, final String iid, String crawlId) throws Exception {
		
		
		final Indexer indexer = index(iid).newIndexer() ;
		final AtomicLong count = new AtomicLong() ;
		indexer.index(isession -> {
			rsession.pathBy(Fqn.fromElements("sites", siteId, crawlId)).children().stream()
				.filter(node -> (node.property("url").asString().endsWith(".html") && (node.property("scode").asInt() == 200))).forEach(node -> {
				try {
					JsonObject json = new JsonObject() ;
					json.put("id", crawlId + "_" + node.fqn().name()) ;
					json.put("crawlid", crawlId) ;
					json.put("cno", Long.parseLong(node.fqn().name())) ;
					json.put("title", node.asString("title")) ;
					json.put("html", node.asString("html")) ;
					json.put("content", node.asString("content")) ;
					JsonArray paths = new JsonArray() ;
					String relUrl = StringUtil.replace(node.asString("url"), node.parent().asString("sieurl"), "");
					String[] pathNames = StringUtil.splitWorker(relUrl, "/") ;
					for(String pathName : pathNames){
						paths.add(JsonPrimitive.createDefault(pathName, "")) ;
					}
					json.put("path", paths) ;
					json.put("relurl", relUrl) ;
					
					JsonArray anchors = new JsonArray() ;
					node.children().stream().forEach(cnode -> {
						anchors.add(JsonPrimitive.createDefault(cnode.asString("anchor"), "")) ;
					});
					json.put("anchor", anchors) ;
					
					WriteDocument wdoc = isession.newDocument(json.asString("id")) ;
					sinfos.addFields(wdoc, json) ;
					isession.updateDocument(wdoc) ;
					
					if ((count.incrementAndGet()) % 100 == 0) isession.commit();
				} catch(IOException e) {
					throw new IllegalStateException(e) ;
				}
			});
			
			return null ;
		}) ;


	}
	

	
	
	public void makeCapture(ReadSession rsession, final String siteId, final String crawlId) throws Exception {
//		System.setProperty("webdriver.chrome.driver", "d:/icsdata/chromedriver.exe");
//		System.setProperty("niss.site.screenHome", "d:/icsdata/logs/");
		rsession.pathBy(Fqn.fromElements("sites", siteId, crawlId)).children().stream()
			.filter(node -> (node.property("url").asString().endsWith(".html") && (node.property("scode").asInt() == 200))).transform(new Function<Iterable<ReadNode>, Integer>() {
			@Override
			public Integer apply(Iterable<ReadNode> iter) {

				WebDriver driver = new ChromeDriver();
				File homeDir = new File(System.getProperty("niss.site.screenHome", "/temp/"), crawlId);

				int count = 0 ;
				try {
					for (ReadNode node : iter) {
						String filename = StringUtil.replace(node.asString("url"), node.parent().asString("siteurl"), "") + ".png";
						driver.get(node.asString("url"));
						
						logger.info("Page title is: " + driver.getTitle());
						logger.info("current URL  : " + driver.getCurrentUrl());
	
						File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
						FileUtils.copyFile(scrFile, new File(homeDir, filename));
						count++ ;
					}
				} catch(IOException e){
					throw new IllegalStateException(e) ;
				} finally {
					driver.quit();
				}
				return count;
			}
		}) ;
	}

	public void indexRemove(final String iid, final String crawlid) {
		index(iid).newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.deleteTerm(new Term("crawlid", crawlid)) ;
				return null;
			}
		}) ;
	}
	
}


