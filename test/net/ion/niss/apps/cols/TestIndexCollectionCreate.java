package net.ion.niss.apps.cols;

import net.ion.framework.util.ListUtil;
import net.ion.niss.apps.collection.IndexCollectionApp;
import net.ion.niss.apps.collection.IndexCollection;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;

import junit.framework.TestCase;

public class TestIndexCollectionCreate extends TestCase {
	private IndexCollectionApp ca;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		this.ca = IndexCollectionApp.create() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		ca.shutdown() ;
		super.tearDown();
	}
	
	public void testAnalyzer() throws Exception {
		assertEquals(false, ca.hasCollection("temp")) ;
		IndexCollection ic = ca.newCollection("temp"); ;
		assertEquals(true, ca.hasCollection("temp")) ;
		
		assertEquals(MyKoreanAnalyzer.class, ic.indexAnalyzer().getClass());
		assertEquals(MyKoreanAnalyzer.class, ic.queryAnalyzer().getClass());


		ic.indexAnalyzer(new CJKAnalyzer(ca.version(), new CharArraySet(ca.version(), ListUtil.newList(), true))) ;
		ic.queryAnalyzer(new CJKAnalyzer(ca.version(), new CharArraySet(ca.version(), ListUtil.newList(), true))) ;

		assertEquals(CJKAnalyzer.class, ic.indexAnalyzer().getClass()) ;
		assertEquals(CJKAnalyzer.class, ic.queryAnalyzer().getClass()) ;
		
		assertEquals(CJKAnalyzer.class.getCanonicalName(), ic.infoNode().property("indexanalyzer").asString()) ;
		assertEquals(CJKAnalyzer.class.getCanonicalName(), ic.infoNode().property("queryanalyzer").asString()) ;
		
		ca.removeCollection("temp") ;
		
		
		assertEquals(false, ca.hasCollection("temp")) ;
	}
	
}
