package net.ion.niss.apps.cols;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.niss.apps.collection.IndexCollectionApp;
import net.ion.niss.apps.collection.IndexCollection;

public class TestIndexAnalysis extends TestCase {
	
	private IndexCollectionApp ca;
	private IndexCollection ic ;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		this.ca = IndexCollectionApp.create() ;
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

	public void testIndexAnalyzer() throws Exception {
		Debug.line(ic.analyzerList()) ;
	}
	
}
