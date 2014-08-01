package net.ion.niss.apps.cols;

import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.niss.apps.CollectionApp;
import net.ion.niss.apps.FieldSchema;
import net.ion.niss.apps.IndexCollection;

public class TestQuery extends TestCase{

	private CollectionApp ca;
	private IndexCollection ic;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		this.ca = CollectionApp.create() ;
		if (! ca.hasCollection("document")){
			ca.newCollection("document") ;
		}
		this.ic = ca.find("document") ;
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		ca.shutdown() ;
		super.tearDown();
	}
	
	public void testIndexSample() throws Exception {
		InputStream input = TestDocument.class.getResourceAsStream("sample.json") ;
		JsonReader jreader = new JsonReader(new InputStreamReader(input)) ;
		
		ic.index(FieldSchema.DEFAULT, jreader) ;
		
		jreader.close(); 
		ic.searcher().search("").debugPrint(); 
	}
	
	
	public void testQueryAll() throws Exception {
		ic.searcher().createRequest("*:*")
			.skip(0).offset(4).sort("id desc")
			.find().debugPrint(); 
	}
	
}
