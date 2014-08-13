package net.ion.niss.webapp.collection;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.radon.core.let.PathHandler;

public class TestOverview extends TestBaseWeb  {

	
	public void testStatus() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/overview").get() ;
		
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals(true, json.has("info")) ;
		assertEquals(true, json.has("status")) ;
		assertEquals(true, json.has("dirInfo")) ;
		
		
		Debug.line(response.contentsString());
	}
	
	
	public void testExplain() throws Exception {
		ss.request("/collections/col1/info").postParam("field", "info").postParam("content", "info 하").post();
		
		StubHttpResponse response = ss.request("/collections/col1/overview").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals("info 하", json.asString("info")) ;
	}
	
	
	public void testUpdateAnalyzer() throws Exception {
		ss.request("/collections/col1/fields").postParam("field", "indexanalyzer").postParam("content", StandardAnalyzer.class.getCanonicalName()).post();
		ss.request("/collections/col1/fields").postParam("field", "applystopword").postParam("content", "true").post();

		StubHttpResponse response = ss.request("/collections/col1/overview").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals(true, json.has("indexanalyzer")) ;
		assertEquals(StandardAnalyzer.class.getCanonicalName(), json.asString("indexanalyzer")) ;
		assertEquals(true, json.has("applystopword")) ;
		assertEquals("true", json.asString("applystopword")) ;
		
		Debug.line(json.asJsonArray("analyzer")) ;
		
	}
	
	
	
	
	public void xtestServer() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(9500).add(new PathHandler(OldCollectionWeb.class).prefixURI("/admin")).start().get() ;
		new InfinityThread().startNJoin(); 
	}
}
