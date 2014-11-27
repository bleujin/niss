package net.ion.niss.webapp.searchers;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.misc.MenuWeb;
import net.ion.niss.webapp.util.WebUtil;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class TestSearcherWeb extends TestBaseSearcher {

	public void testListSection() throws Exception {
		
		StubHttpResponse response = ss.request("/searchers").get() ;
		assertEquals("sec1",  new JsonParser().parse(response.contentsString()).getAsJsonArray().get(0).getAsJsonObject().asString("sid"));
	}
	
	public void testEditSection() throws Exception {
		StubHttpResponse response = ss.request("/searchers/sec1/define")
			.postParam("target", "document,abcd")
			.postParam("queryanalyzer", StandardAnalyzer.class.getCanonicalName())
			.postParam("handler", "function()").postParam("applyhandler", "false")
			.post() ;
		
		assertEquals("defined searcher : sec1", response.contentsString());
		
		
		response = ss.request("/searchers/sec1/define").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		
		Debug.line(json);
		assertEquals("document", json.asJsonArray("target").toObjectArray()[0]) ;
		assertEquals("abcd", json.asJsonArray("target").toObjectArray()[1]) ;
		assertEquals("function()", json.asString("handler")) ;
		assertEquals("false", json.asString("applyhandler")) ;
	}
	
	
	public void testViewSample() throws Exception {
		assertEquals(3, WebUtil.findLoaderScripts().size());
		assertEquals(true, WebUtil.findSearchHandlers().size() > 0);
		assertEquals(true, WebUtil.findSearchTemplates().size() > 0);
		assertEquals(true, WebUtil.findScripts().size() > 0);
	}
}
