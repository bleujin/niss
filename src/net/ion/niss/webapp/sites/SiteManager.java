package net.ion.niss.webapp.sites;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.ion.craken.node.ReadNode;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.db.procedure.IUserProcedureBatch;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.icrawler.Spider;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.common.Def.IndexSchema;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.SchemaInfos;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.SearchResponse;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.Term;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import com.google.common.base.Function;

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

	public void crawlSite(final IDBController dc, final CrawlOption coption) throws Exception {

		new Thread(){
			public void run(){
				Spider spider = null ;
				try {
					SiteProcessor processor = SiteProcessor.create(dc, coption.siteUrl(), coption);
					spider = processor.newSpider();
					spider.run(); // .setScheduler(new MaxLimitScheduler(new QueueScheduler(), 20))
				} catch (SQLException e) { e.printStackTrace(); 
				} finally {
					if (spider != null) spider.close(); 
				}
			}
		}.start(); 
	}
	
	
	public void indexCrawlSite(final IDBController dc, final SchemaInfos sinfos, final String iid, String crawlId) throws Exception {

		dc.createUserProcedure("Crawl@htmlContentListBy(?)").addParam(crawlId).execHandlerQuery(new ResultSetHandler<Integer>() {

			@Override
			public Integer handle(final ResultSet rs) throws SQLException {
				final Indexer indexer = index(iid).newIndexer() ;

				indexer.index(new IndexJob<Void>() {

					@Override
					public Void handle(IndexSession isession) throws Exception {

						int count = 0 ;
						while(rs.next()){
							// select x1.crawlId, cno, url, replace(url, x2.siteUrl, '') relUrl, urlhash, scode, title, screenPath, html, content
							JsonObject json = new JsonObject() ;
							json.put("id", rs.getString("crawlId") + "_" + rs.getString("cno")) ;
							json.put("crawlid", rs.getString("crawlId")) ;
							json.put("cno", rs.getLong("cno")) ;
							json.put("title", rs.getString("title")) ;
							json.put("html", rs.getString("html")) ;
							json.put("content", rs.getString("content")) ;
							JsonArray paths = new JsonArray() ;
							String[] pathNames = StringUtil.splitWorker(rs.getString("relUrl"), "/") ;
							for(String pathName : pathNames){
								paths.add(JsonPrimitive.createDefault(pathName, "")) ;
							}
							json.put("path", paths) ;
							json.put("relurl", rs.getString("relUrl")) ;
							
							Rows rows = dc.createUserProcedure("CRAWL@referContentlistBy(?,?)").addParam(rs.getString("crawlId")).addParam(rs.getString("url")).execQuery() ;
							JsonArray anchors = new JsonArray() ;
							while(rows.next()){
								anchors.add(JsonPrimitive.createDefault(rows.getString("anchor"), "")) ;
							}
							json.put("anchor", anchors) ;
							
							WriteDocument wdoc = isession.newDocument(json.asString("id")) ;
							sinfos.addFields(wdoc, json) ;
							isession.updateDocument(wdoc) ;
							
							if ((++count) % 100 == 0) isession.commit(); 
						}
						return null;
					}
				}) ;
				
				return null;
			}
		}) ;
	}
	

	
	
	public void makeCapture(IDBController dc, final String crawlId) throws Exception {
//		System.setProperty("webdriver.chrome.driver", "d:/icsdata/chromedriver.exe");
//		System.setProperty("niss.site.screenHome", "d:/icsdata/logs/");
		
		dc.createUserProcedure("Crawl@htmlListBy(?)").addParam(crawlId).execHandlerQuery(new ResultSetHandler<Integer>() {
			public Integer handle(ResultSet rs) throws SQLException {
				WebDriver driver = new ChromeDriver();
				File homeDir = new File(System.getProperty("niss.site.screenHome", "/temp/"), crawlId);

				int count = 0 ;
				try {
				while (rs.next()) {
					String filename = rs.getString("path");
					driver.get(rs.getString("url"));
					
					logger.info("Page title is: " + driver.getTitle());
					logger.info("current URL  : " + driver.getCurrentUrl());

					File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
					FileUtils.copyFile(scrFile, new File(homeDir, filename));
					count++ ;
				}
				} catch(IOException e){
					throw new SQLException(e) ;
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


