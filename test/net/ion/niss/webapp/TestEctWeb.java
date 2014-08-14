package net.ion.niss.webapp;

import org.apache.lucene.analysis.util.CharArraySet;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.niss.webapp.MenuWeb;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.TemplateWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.radon.client.StubServer;

public class TestEctWeb extends TestCase {

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(MenuWeb.class, TemplateWeb.class, AnalysisWeb.class, TunnelWeb.class);
		ss.treeContext().putAttribute(REntry.EntryName, REntry.test());
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
		JsonArray array = JsonParser.fromString(response.contentsString()).getAsJsonArray() ;
		JsonObject json = array.get(0).getAsJsonObject() ;
		
		assertEquals(true, json.has("clz"));
		assertEquals(true, json.has("name"));
	}

	
	public void testExecuteAnalysis() throws Exception {
		StubHttpResponse response = ss.request("/analysis").postParam("content", "태극기가 바람에 펄럭입니다").postParam("analyzer", MyKoreanAnalyzer.class.getCanonicalName()).postParam("stopword", "바람") .post() ;
		JsonArray array = JsonParser.fromString(response.contentsString()).getAsJsonArray() ;
		
		assertEquals(true, array.size() > 0);
		Debug.line(response.contentsString()) ;
	}
	
	
	public void testTunnel() throws Exception {
		StubHttpResponse response = ss.request("/tunnel/emps/bleujin").postParam("name", "bleujin").postParam("age", "20").post() ;
		
		assertEquals("emps/bleujin edited" , response.contentsString()) ;
		
		response = ss.request("/tunnel/emps/bleujin").get() ;
		Debug.line(response.contentsString());
	}
}
