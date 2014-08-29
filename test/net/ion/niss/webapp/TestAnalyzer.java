package net.ion.niss.webapp;

import java.lang.reflect.Constructor;
import java.util.List;

import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

import junit.framework.TestCase;

public class TestAnalyzer extends TestCase{

	
	public void testSimple() throws Exception {
		
		List<Class<? extends Analyzer>> alist = ListUtil.newList() ;
		alist.add(SimpleAnalyzer.class) ;
		alist.add(MyKoreanAnalyzer.class) ;
		alist.add(CJKAnalyzer.class) ;
		alist.add(StandardAnalyzer.class) ;
		alist.add(WhitespaceAnalyzer.class) ;
		
		for (Class<? extends Analyzer> anal : alist) {
//			Debug.line(anal.getDeclaredConstructor(Version.class, CharArraySet.class)) ;
			
			Constructor findCon = ConstructorUtils.getAccessibleConstructor(anal, new Class[]{Version.class, CharArraySet.class}) ;
			if (findCon == null) findCon = ConstructorUtils.getAccessibleConstructor(anal, new Class[]{Version.class}) ;
			Debug.debug(anal, findCon);
		}
		
	}

}
