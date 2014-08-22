package net.ion.niss.webapp.loaders;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import junit.framework.TestCase;
import net.ion.icrawler.Site;
import net.ion.icrawler.Spider;
import net.ion.icrawler.pipeline.DebugPipeline;
import net.ion.icrawler.processor.SimplePageProcessor;
import net.ion.icrawler.scheduler.MaxLimitScheduler;
import net.ion.icrawler.scheduler.QueueScheduler;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.loaders.ExceptionHandler;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;

public class TestFromCrawler extends TestCase {


	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testCreate() throws Exception {
		JScriptEngine app = JScriptEngine.create() ;
		
		InstantJavaScript script = app.createScript(IdString.create("crawl_bleujin"), "Sample From Crawl", new FileInputStream("./resource/loader/sample_crawl.script")) ;
		
		final Writer writer =  new OutputStreamWriter(System.out, "UTF-8");
		script.run(writer, new ExceptionHandler(){
			@Override
			public Void handle(Exception ex) {
				try {
					writer.write(ex.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null ;
			}
		}) ;
		writer.flush(); 
		writer.close();
	}
	
	public void testRun() throws Exception {
		 Spider spider = Site.create().sleepTime(50).newSpider(new SimplePageProcessor("http://bleujin.tistory.com/*")).scheduler(new MaxLimitScheduler(new QueueScheduler(), 10)).addUrl("http://bleujin.tistory.com/") ;
		 spider.addPipeline(new DebugPipeline()).run();
	}
}
