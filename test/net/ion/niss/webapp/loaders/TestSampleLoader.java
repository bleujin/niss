package net.ion.niss.webapp.loaders;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.icrawler.Site;
import net.ion.icrawler.Spider;
import net.ion.icrawler.pipeline.DebugPipeline;
import net.ion.icrawler.processor.SimplePageProcessor;
import net.ion.icrawler.scheduler.MaxLimitScheduler;
import net.ion.icrawler.scheduler.QueueScheduler;
import net.ion.niss.webapp.IdString;

public class TestSampleLoader extends TestCase {


	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	
	// database 
	
	public void testBean() throws Exception {
		final Writer writer = new OutputStreamWriter(System.out, "EUC-KR");
		final JsonWriter jwriter = new JsonWriter(writer);

		RDB.oracle("61.250.201.239:1521:qm10g", "bleujin", "redf").query("select * from tabs").handle(new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				try {
					jwriter.beginArray();

					while (rs.next()) {
						jwriter.beginObject();
						jwriter.name("table_name").value(rs.getString("table_name"));
						jwriter.endObject();
					}
					jwriter.endArray();
					jwriter.close();
				} catch (IOException e) {
					throw new SQLException(e);
				}
				return null;
			}
		});

	}

	public void testFromDB() throws Exception {
		JScriptEngine app = JScriptEngine.create() ;
		
		InstantJavaScript script = app.createScript(IdString.create("sample_db"), "Sample From DB", JScriptEngine.class.getResourceAsStream("fromdb.txt")) ;
		
		Writer writer = new StringWriter();
		script.execAsync(ResultHandler.DEFAULT, writer) ;
		
		Debug.line(writer);
	}
	
	
	
	// crawler
	public void testRun() throws Exception {
		 Spider spider = Site.create().sleepTime(50).newSpider(new SimplePageProcessor("http://bleujin.tistory.com/*")).scheduler(new MaxLimitScheduler(new QueueScheduler(), 10)).addUrl("http://bleujin.tistory.com/") ;
		 spider.addPipeline(new DebugPipeline()).run();
	}

	public void testRunCrawlScript() throws Exception {
		JScriptEngine app = JScriptEngine.create() ;
		
		InstantJavaScript script = app.createScript(IdString.create("crawl_bleujin"), "Sample From Crawl", new FileInputStream("./resource/loader/crawl_sample.script")) ;
		
		final Writer writer =  new OutputStreamWriter(System.out, "UTF-8");
		script.exec(new ResultHandler<Void>(){
			@Override
			public Void onSuccess(Object result, Object... args) {
				IOUtil.close(writer);
				return null;
			}
			@Override
			public Void onFail(Exception ex, Object... args) {
				try {
					writer.write(ex.getMessage());
					IOUtil.closeQuietly(writer);
				} catch (IOException e) {
				}
				return null;
			}
		}, writer) ;
	}
	
}
