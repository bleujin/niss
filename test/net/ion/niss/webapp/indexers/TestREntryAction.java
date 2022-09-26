package net.ion.niss.webapp.indexers;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import net.bleujin.rcraken.ReadSession;
import net.bleujin.searcher.SearchController;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.nradon.stub.StubHttpResponse;

public class TestREntryAction extends TestBaseIndexWeb {

	public void testAnalyzer() throws Exception {
		IndexManager im = entry.indexManager() ;
		IdString cid = IdString.create("col1");
		assertEquals(true, im.hasIndex(cid)) ;
		
		assertEquals(StandardAnalyzer.class, im.index(cid).defaultIndexConfig().analyzer().getClass()) ;
	}
	
	public void testRemoveIndex() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1").delete();
		
		IndexManager im = entry.indexManager() ;
		IdString cid = IdString.create("col1");
		assertEquals(false, im.hasIndex(cid)) ;
	}
	
	
	public void testChangeIndexAnalyzer() throws Exception {
		IndexManager im = entry.indexManager() ;
		SearchController cen = im.index("col1") ;

		assertEquals(StandardAnalyzer.class, cen.defaultIndexConfig().analyzer().getClass()) ;
		
		ReadSession session = entry.login() ;
		session.tran(wsession -> {
			wsession.pathBy("/indexers/col1").property(Def.Indexer.IndexAnalyzer, KeywordAnalyzer.class.getCanonicalName()).merge();
		}).get() ;
		
		assertEquals(KeywordAnalyzer.class, cen.defaultIndexConfig().analyzer().getClass()) ;

		
		session.tran(wsession -> {
			wsession.pathBy("/indexers/col1").property(Def.Indexer.IndexAnalyzer, CJKAnalyzer.class.getCanonicalName()).merge();
		}).get() ;
		
		assertEquals(CJKAnalyzer.class, cen.defaultIndexConfig().analyzer().getClass()) ;
	}

	
	public void testChangeQueryAnalyzer() throws Exception {
		IndexManager im = entry.indexManager() ;
		SearchController cen = im.index("col1") ;

		assertEquals(StandardAnalyzer.class, cen.defaultSearchConfig().queryAnalyzer().getClass()) ;
		
		ReadSession session = entry.login() ;
		session.tran(wsession -> {
			wsession.pathBy("/indexers/col1").property(Def.Indexer.QueryAnalyzer, KeywordAnalyzer.class.getCanonicalName()).merge();
		}).get() ;
		
		assertEquals(KeywordAnalyzer.class, cen.defaultSearchConfig().queryAnalyzer().getClass()) ;

		
		session.tran(wsession -> {
			wsession.pathBy("/indexers/col1").property(Def.Indexer.QueryAnalyzer, CJKAnalyzer.class.getCanonicalName()).merge();
		}).get() ;
		
		assertEquals(CJKAnalyzer.class, cen.defaultSearchConfig().queryAnalyzer().getClass()) ;
	}
	

	
	public void testApplyStopword() throws Exception {
		IndexManager im = entry.indexManager() ;
		SearchController cen = im.index("col1") ;
		
		JsonArray jarray = AnalysisWeb.analParse(cen.defaultIndexConfig().analyzer(), "태극기가 바람에 펄럭입니다") ;
		assertEquals("태극기가", jarray.get(0).getAsJsonObject().asString("term"));
		assertEquals("바람에", jarray.get(1).getAsJsonObject().asString("term"));
		assertEquals("펄럭입니다", jarray.get(2).getAsJsonObject().asString("term"));

		
		ReadSession session = entry.login() ;
		session.tran(wsession -> {
			wsession.pathBy("/indexers/col1")
				.property(Def.Indexer.IndexAnalyzer, CJKAnalyzer.class.getCanonicalName())
				.property(Def.Indexer.ApplyStopword, "true")
				.property(Def.Indexer.StopWord, "바람").merge();
		}).get() ;

		JsonArray marray = AnalysisWeb.analParse(cen.defaultIndexConfig().analyzer(), "태극기가 바람에 펄럭입니다") ;
		Debug.line(marray);
		for (JsonElement je : marray.toArray()) {
			if ("바람".equals(je.getAsJsonObject().asString("term"))) fail(); 
		}

		session.tran(wsession -> {
			wsession.pathBy("/indexers/col1")
				.property(Def.Indexer.ApplyStopword, "false").merge();
		}).get() ;


		marray = AnalysisWeb.analParse(cen.defaultIndexConfig().analyzer(), "태극기가 바람에 펄럭입니다") ;
		for (JsonElement je : marray.toArray()) {
			if ("바람".equals(je.getAsJsonObject().asString("term"))) return ; 
		}

		fail();
	}
	
	public void testEditStopword() throws Exception {
		
	}
	
	
}
