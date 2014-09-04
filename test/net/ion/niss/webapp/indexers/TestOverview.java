package net.ion.niss.webapp.indexers;

import org.apache.solr.response.transform.RenameFieldTransformer;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.config.Central;

public class TestOverview extends TestBaseIndexWeb {

	
	public void testOverviewCall() throws Exception {
		StubHttpResponse response = ss.request("/indexers/col1/overview").get() ;
		
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals(true, json.has("info"));
		assertEquals(true, json.has("status"));
		assertEquals(true, json.has("dirInfo"));
		
		Debug.line(response.contentsString());
	}
	

		
}
