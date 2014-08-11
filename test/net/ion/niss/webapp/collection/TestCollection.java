package net.ion.niss.webapp.collection;

import net.ion.framework.util.Debug;
import net.ion.nradon.stub.StubHttpResponse;

public class TestCollection extends TestBaseWeb {

	
	public void testListCollection() throws Exception {
		StubHttpResponse response = ss.request("/collections").get() ;
		
//		assertEquals(MediaType.APPLICATION_JSON, response.header(HttpHeaderNames.CONTENT_TYPE));
		Debug.line(response.contentsString());
	}
	
	public void testCreateCollection() throws Exception {
		ss.request("/collections/col2").post() ;
		assertEquals(true, ca.hasCollection("col2")) ;
	}
}
