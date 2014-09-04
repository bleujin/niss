package net.ion.niss.webapp.indexers;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.common.Def;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

public class TestDefined extends TestBaseIndexWeb {


	
	public void testDefineIndexer() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/defined")
					.postParam(Def.Indexer.IndexAnalyzer, MyKoreanAnalyzer.class.getCanonicalName())
					.postParam("stopword", "bleu jin hero")
					.postParam("applystopword", "true")
					.postParam(Def.Indexer.QueryAnalyzer,  MyKoreanAnalyzer.class.getCanonicalName())
					.post() ;
		assertEquals("defined indexer : col1", response.contentsString());

		
		response = ss.request("/indexers/col1/defined").get() ;

		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		
		assertEquals(true, json.has(Def.Indexer.IndexAnalyzer) == true);
		assertEquals(true, json.has(Def.Indexer.StopWord) == true);
		assertEquals(true, json.has(Def.Indexer.ApplyStopword) == true);
		assertEquals(true, json.has(Def.Indexer.QueryAnalyzer) == true);
	}
	
		
}
