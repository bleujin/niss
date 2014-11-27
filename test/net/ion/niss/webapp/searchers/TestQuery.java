package net.ion.niss.webapp.searchers;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.nradon.stub.StubHttpResponse;

public class TestQuery extends TestBaseSearcher {

	public void testQuery() throws Exception {
		rentry.searchManager().searcher("sec1").search("").debugPrint();
	}
	
	public void testJson() throws Exception {
		StubHttpResponse response = ss.request("/searchers/sec1/query.json?query=").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString());
		assertEquals(0, json.asJsonArray("docs").size());
	}
	
	
}
