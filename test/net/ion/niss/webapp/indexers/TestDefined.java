package net.ion.niss.webapp.indexers;

import org.apache.lucene.analysis.ko.MyKoreanAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import net.bleujin.rcraken.ReadSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.webapp.common.Def;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

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
		
		
		ReadSession session = entry.login() ;
		assertEquals("bleu jin hero", session.pathBy("/indexers/col1").property("stopword").asString());
	}
	
	
	public void testDefineStopword() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/defined")
				.postParam(Def.Indexer.IndexAnalyzer, StandardAnalyzer.class.getCanonicalName())
				.postParam("stopword", "company cafe")
				.postParam("applystopword", "true")
				.postParam(Def.Indexer.QueryAnalyzer,  StandardAnalyzer.class.getCanonicalName())
				.post() ;
		
		Central central = entry.indexManager().index("col1") ;
		
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.newDocument("newdoc").text("explain", "company cafe house").update() ;
				return null;
			}
		}) ;
		
		assertEquals(0, central.newSearcher().search("explain:company").totalCount()) ; 
		assertEquals(1, central.newSearcher().search("explain:house").totalCount()) ;
		
		central.close(); 
	}
		
}
