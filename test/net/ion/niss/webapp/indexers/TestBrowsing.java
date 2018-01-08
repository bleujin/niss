package net.ion.niss.webapp.indexers;

import java.net.URLDecoder;
import java.util.List;

import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.Debug;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.config.Central;

public class TestBrowsing extends TestBaseIndexWeb {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ss.request("/indexers/col1/schema").postParam("schemaid", "name").postParam("schematype", "text").postParam("analyze", "false").postParam("store", "true").postParam("boost", "1.0").post() ;
		ss.request("/indexers/col1/schema").postParam("schemaid", "age").postParam("schematype", "number").postParam("analyze", "true").postParam("store", "true").postParam("boost", "1.0").post() ;
		ss.request("/indexers/col1/schema").postParam("schemaid", "address").postParam("schematype", "text").postParam("analyze", "true").postParam("store", "true").postParam("boost", "1.0").post() ;
	}
	
	public void testSchema() throws Exception {
		ReadSession session = entry.login() ;
		session.pathBy("/indexers/col1/schema").children().debugPrint(); 
	}
	
	public void testSearchField() throws Exception {
		Central ic = entry.indexManager().index("col1") ;
		List<ReadDocument> docs = ic.newSearcher().createRequest("").selections("id", "age", "address", "name").find().getDocument() ; //.debugPrint();
		for (ReadDocument doc : docs) {
			Debug.line(doc.asString("name"), doc.asString("id"), doc.asString("age"));
		}
		
	}
	
	public void testCall() throws Exception {
		
		StubHttpResponse response = ss.request("/indexers/col1/browsing?searchQuery=").get() ;
		Debug.line(response);
	} 

	
	public void testChar() throws Exception {
		Debug.line(URLDecoder.decode("columns%5B4%5D%5Bdata%5D=4")) ;
	}

}
