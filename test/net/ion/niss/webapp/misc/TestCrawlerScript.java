package net.ion.niss.webapp.misc;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.core.Response;

import net.ion.framework.parse.gson.Gson;
import net.ion.framework.parse.gson.GsonBuilder;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.icrawler.Page;
import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Site;
import net.ion.icrawler.Spider;
import net.ion.icrawler.Task;
import net.ion.icrawler.pipeline.Pipeline;
import net.ion.icrawler.processor.PageProcessor;
import net.ion.icrawler.scheduler.MaxLimitScheduler;
import net.ion.icrawler.scheduler.QueueScheduler;
import net.ion.icrawler.selector.Link;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.indexers.TestBaseIndexWeb;
import net.ion.niss.webapp.loaders.JScriptEngine;

public class TestCrawlerScript extends TestBaseIndexWeb {

	public void testToION() throws Exception {
		Site site = Site.create("http://www.i-on.net/index.html").sleepTime(50);

		String hostPattern = "http://www.i-on.net/*";
		final String urlPattern = (new StringBuilder("(")).append(hostPattern.replace(".", "\\.").replace("*", "[^\"'#]*")).append(")").toString();

		PageProcessor processor = new PageProcessor() {
			public void process(Page page) {
				List links = page.getHtml().links().regex(urlPattern).targets();
				
				
				JsonObject json = new JsonObject() ;
				json.put("url", page.getRequest().getUrl()) ;
				json.put("title", page.getHtml().xpath("//title").get()) ;
				json.put("images", new JsonArray().adds(page.getHtml().xpath("//img/@src").all().toArray(new String[0]))) ;
				json.put("links", new JsonArray().adds(links.toArray(new Link[0])));
				
				page.putField("result", json);
				page.addTargets(links);
			}
		};

		final Gson gson = new GsonBuilder().setPrettyPrinting().create() ;
		final StringWriter writer = new StringWriter(16000);
		Pipeline debug = new Pipeline() {
			@Override
			public void process(ResultItems ritems, Task task) {
				JsonObject json = ritems.asObject("result");
				gson.toJson(json, writer) ;
			}
		};

		Spider spider = site.newSpider(processor).scheduler(new MaxLimitScheduler(new QueueScheduler(), 10));
		spider.addPipeline(debug).run();
		Debug.line(writer);
	}
	
	public void testRunScript() throws Exception {
		runScript(getClass().getResourceAsStream("working.script"));
	}
	
	public void runScript(InputStream input) throws Exception {
		final REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		final JScriptEngine jengine = ss.treeContext().getAttributeObject(JScriptEngine.EntryName, JScriptEngine.class) ;
		ScriptWeb sweb = new ScriptWeb(rentry, jengine) ;
		
		Response response = sweb.instantRunScript(null, IOUtil.toStringWithClose(input)) ;
		Debug.line(response.getEntity()) ;
	}
}
