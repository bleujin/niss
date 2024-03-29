package net.ion.niss.webapp.misc;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.searchers.TemplateWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestEctWeb extends TestCase {

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(MenuWeb.class, TemplateWeb.class, AnalysisWeb.class, TunnelWeb.class);
		ss.treeContext().putAttribute(REntry.EntryName, REntry.testStup());
	}

	@Override
	protected void tearDown() throws Exception {
		ss.shutdown();
		super.tearDown();
	}

	public void testEditInfo() throws Exception {
		ss.request("/menus/sections").postParam("field", "overview").postParam("content", "Hello Overview").post();
		StubHttpResponse response = ss.request("/menus/sections?field=overview").get();
		assertEquals("Hello Overview", response.contentsString());
	}

	public void testEditTemplate() throws Exception {
		StubHttpResponse response = ss.request("/templates/sec1").postParam("content", "dfdf").post();
		assertEquals("edit template", response.contentsString());

		response = ss.request("/templates/sec1").get();
		assertEquals("dfdf", response.contentsString());
	}
	
	public void testAnalysis() throws Exception {
		StubHttpResponse response = ss.request("/analysis").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString());
		
		assertEquals(true, json.has("analyzer"));
	}

	
	public void testExecuteAnalysis() throws Exception {
		StubHttpResponse response = ss.request("/analysis").postParam("content", "태극기가 바람에 펄럭입니다").postParam("analyzer", KoreanAnalyzer.class.getCanonicalName()).postParam("stopword", "바람") .post() ;
		JsonObject jo = JsonParser.fromString(response.contentsString()).getAsJsonObject() ;
		
		Debug.line(response.contentsString()) ;
		assertEquals(true, jo.keySet().size() > 0);
	}
	
	public void testExecuteNoStopwordAnalyzer() throws Exception {
		StubHttpResponse response = ss.request("/analysis").postParam("content", "태극기가 바람에 펄럭입니다").postParam("analyzer", SimpleAnalyzer.class.getCanonicalName()).postParam("stopword", "바람") .post() ;
		JsonObject jo = JsonParser.fromString(response.contentsString()).getAsJsonObject() ;
		
		assertEquals(true, jo.keySet().size() > 0);
		Debug.line(response.contentsString()) ;
	}
	
	
	
	public void testTunnel() throws Exception {
		StubHttpResponse response = ss.request("/tunnel/emps/bleujin").postParam("name", "bleujin").postParam("age", "20").post() ;
		
		assertEquals("emps/bleujin edited" , response.contentsString()) ;
		
		response = ss.request("/tunnel/emps/bleujin").get() ;
		Debug.line(response.contentsString());
	}
}
