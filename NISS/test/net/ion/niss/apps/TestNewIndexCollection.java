package net.ion.niss.apps;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;

public class TestNewIndexCollection extends TestCase {

	private CollectionApp ca;
	private IndexCollection ic;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		this.ca = CollectionApp.create() ;
		this.ic = ca.newCollection("col1") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		ca.shutdown() ;
		super.tearDown();
	}

	
	public void testAnalyzer() throws Exception {
		assertEquals(MyKoreanAnalyzer.class, ic.indexAnalyzer().getClass());
		assertEquals(MyKoreanAnalyzer.class, ic.queryAnalyzer().getClass());


		ic.indexAnalyzer(new CJKAnalyzer(ca.version(), new CharArraySet(ca.version(), ListUtil.newList(), true))) ;
		ic.queryAnalyzer(new CJKAnalyzer(ca.version(), new CharArraySet(ca.version(), ListUtil.newList(), true))) ;

		assertEquals(CJKAnalyzer.class, ic.indexAnalyzer().getClass()) ;
		assertEquals(CJKAnalyzer.class, ic.queryAnalyzer().getClass()) ;
		
		assertEquals(CJKAnalyzer.class.getCanonicalName(), ic.infoNode().property("indexanalyzer").asString()) ;
		assertEquals(CJKAnalyzer.class.getCanonicalName(), ic.infoNode().property("queryanalyzer").asString()) ;
	}
	
	
	public void testUpdateDocument() throws Exception {
		JsonObject jo = JsonObject.create().put("name", "bleujin").put("age", 20) ;
		
		ic.mergeDocument("bleujin", jo) ;
		
		ReadDocument rdoc = ic.findNode("bleujin") ;
		assertEquals("bleujin", rdoc.get("name")) ;
		assertEquals(20, rdoc.getAsLong("age"));
	}
	
	
	
	
	
}
