package net.ion.niss.webapp.indexers;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.IndexerWeb;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.radon.client.StubServer;

public class TestREntryAction extends TestBaseIndexWeb {

	public void testAnalyzer() throws Exception {
		IndexManager im = entry.indexManager() ;
		IdString cid = IdString.create("col1");
		assertEquals(true, im.hasIndex(cid)) ;
		
		assertEquals(StandardAnalyzer.class, im.index(cid).newIndexer().analyzer().getClass()) ;
	}
	
	public void testRemoveIndex() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1").delete();
		
		IndexManager im = entry.indexManager() ;
		IdString cid = IdString.create("col1");
		assertEquals(false, im.hasIndex(cid)) ;
	}
	
	
	public void testChangeIndexAnalyzer() throws Exception {
		IndexManager im = entry.indexManager() ;
		Central cen = im.index("col1") ;

		assertEquals(StandardAnalyzer.class, cen.indexConfig().indexAnalyzer().getClass()) ;
		
		ReadSession session = entry.login() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/indexers/col1").property(Def.Indexer.IndexAnalyzer, KeywordAnalyzer.class.getCanonicalName()) ;
				return null;
			}
		}).get() ;
		
		assertEquals(KeywordAnalyzer.class, cen.indexConfig().indexAnalyzer().getClass()) ;

		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/indexers/col1").property(Def.Indexer.IndexAnalyzer, CJKAnalyzer.class.getCanonicalName()) ;
				return null;
			}
		}).get() ;
		
		assertEquals(CJKAnalyzer.class, cen.indexConfig().indexAnalyzer().getClass()) ;
	}

	
	public void testChangeQueryAnalyzer() throws Exception {
		IndexManager im = entry.indexManager() ;
		Central cen = im.index("col1") ;

		assertEquals(StandardAnalyzer.class, cen.searchConfig().queryAnalyzer().getClass()) ;
		
		ReadSession session = entry.login() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/indexers/col1").property(Def.Indexer.QueryAnalyzer, KeywordAnalyzer.class.getCanonicalName()) ;
				return null;
			}
		}).get() ;
		
		assertEquals(KeywordAnalyzer.class, cen.searchConfig().queryAnalyzer().getClass()) ;

		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/indexers/col1").property(Def.Indexer.QueryAnalyzer, CJKAnalyzer.class.getCanonicalName()) ;
				return null;
			}
		}).get() ;
		
		assertEquals(CJKAnalyzer.class, cen.searchConfig().queryAnalyzer().getClass()) ;
	}
	

	
	public void testApplyStopword() throws Exception {
		IndexManager im = entry.indexManager() ;
		Central cen = im.index("col1") ;
		
		JsonArray jarray = AnalysisWeb.analParse(cen.indexConfig().indexAnalyzer(), "태극기가 바람에 펄럭입니다") ;
		assertEquals("태극기가", jarray.get(0).getAsJsonObject().asString("term"));
		assertEquals("바람에", jarray.get(1).getAsJsonObject().asString("term"));
		assertEquals("펄럭입니다", jarray.get(2).getAsJsonObject().asString("term"));

		
		ReadSession session = entry.login() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/indexers/col1")
					.property(Def.Indexer.IndexAnalyzer, CJKAnalyzer.class.getCanonicalName())
					.property(Def.Indexer.ApplyStopword, "true")
					.property(Def.Indexer.StopWord, new String[]{"바람"});
				return null;
			}
		}).get() ;

		JsonArray marray = AnalysisWeb.analParse(cen.indexConfig().indexAnalyzer(), "태극기가 바람에 펄럭입니다") ;
		Debug.line(marray);
		for (JsonElement je : marray.toArray()) {
			if ("바람".equals(je.getAsJsonObject().asString("term"))) fail(); 
		}

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/indexers/col1")
					.property(Def.Indexer.ApplyStopword, "false") ;
				return null;
			}
		}).get() ;


		marray = AnalysisWeb.analParse(cen.indexConfig().indexAnalyzer(), "태극기가 바람에 펄럭입니다") ;
		for (JsonElement je : marray.toArray()) {
			if ("바람".equals(je.getAsJsonObject().asString("term"))) return ; 
		}

		fail();
	}
	
	public void testEditStopword() throws Exception {
		
	}
	
	
}