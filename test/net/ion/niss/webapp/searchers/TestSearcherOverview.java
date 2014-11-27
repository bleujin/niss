package net.ion.niss.webapp.searchers;

import net.ion.craken.node.ReadSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.nradon.stub.StubHttpResponse;


public class TestSearcherOverview extends TestBaseSearcher {

	public void testSearchLogs() throws Exception {
		
		StubHttpResponse response = ss.request("/searchers/sec1/overview").get() ;
		
		JsonObject json = JsonObject.fromString(response.contentsString()) ; 
		assertEquals(true, json.has("info"));
		assertEquals(true, json.has("recent"));
		assertEquals(true, json.has("popular"));
		
	}

}
