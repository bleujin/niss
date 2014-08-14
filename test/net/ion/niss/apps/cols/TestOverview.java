package net.ion.niss.apps.cols;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.apps.old.IndexCollection;
import net.ion.niss.apps.old.IndexManager;
import net.ion.nsearcher.common.ReadDocument;

public class TestOverview extends TestCase {

	private IndexManager ca;
	private IndexCollection ic;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		this.ca = IndexManager.create() ;
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
		JsonObject json = ic.status() ;
		Debug.line(json);
	}
	
	public void testInstance() throws Exception {
		JsonObject infoJson = ic.dirInfo() ;
		Debug.line(infoJson);
	}
	
	public void testFile() throws Exception {
		Debug.line(ic.fileList()) ;
	}
	
	
	public void testExplain() throws Exception {
		String value = "<pre>Collection은 Index를 관리하는 곳입니다.</pre>";
		ic.updateExplain("overview", value);
		
		assertEquals(value, ic.propAsString("overview")) ;
	}

	
}
