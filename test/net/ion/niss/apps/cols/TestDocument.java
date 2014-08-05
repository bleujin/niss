package net.ion.niss.apps.cols;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.apps.collection.CollectionApp;
import net.ion.niss.apps.collection.IndexCollection;
import net.ion.nsearcher.common.ReadDocument;

public class TestDocument extends TestCase {

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
	
	
	public void testUpdateDocument() throws Exception {
		

		ReadDocument rdoc = ic.searcher().createRequestByKey("bleujin").findOne() ;
		assertEquals("bleujin", rdoc.get("name")) ;
		assertEquals(20, rdoc.getAsLong("age"));
	}
	

	
}
