package net.ion.niss.apps;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.nradon.restlet.data.NamedValue;
import net.ion.nradon.restlet.data.Parameter;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.reader.InfoReader.InfoHandler;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;

public class TestIndexCollection extends TestCase {

	private CollectionApp ca;
	private IndexCollection ic;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		this.ca = CollectionApp.create() ;
		if (! ca.hasCollection("col1")){
			ca.newCollection("col1") ;
		}
		this.ic = ca.find("col1") ;
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		ca.shutdown() ;
		super.tearDown();
	}
	
	
	public void testAvailableAnalyzer() throws Exception {
		assertEquals(true, ca.analyzers().size() > 3) ;
	}

	public void testOverview() throws Exception {
		Map<String, Object> infoMap = ic.status() ;
		Debug.line(infoMap);
	}
	
	public void testInstance() throws Exception {
		Map<String, Object> infoMap = ic.dirInfo() ;
		Debug.line(infoMap);
	}
	
	public void testFile() throws Exception {
		Debug.line(ic.fileList()) ;
	}
	
	public void testUpdateDocument() throws Exception {
		JsonObject jo = JsonObject.create().put("name", "bleujin").put("age", 20) ;
		
		ic.mergeDocument("bleujin", jo) ;
		
		ReadDocument rdoc = ic.findNode("bleujin") ;
		assertEquals("bleujin", rdoc.get("name")) ;
		assertEquals(20, rdoc.getAsLong("age"));
	}
	
	
	
	
	
}
