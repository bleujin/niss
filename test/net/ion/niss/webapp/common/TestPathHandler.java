package net.ion.niss.webapp.common;

import net.ion.framework.util.Debug;
import net.ion.niss.webapp.indexers.TestBaseIndexWeb;
import net.ion.nradon.stub.StubHttpResponse;

public class TestPathHandler extends TestBaseIndexWeb {
	
	public void testInfo() throws Exception {
		
		
		StubHttpResponse response = ss.request("/indexers/col1/status").get() ;
		Debug.line(response.contentsString());
	}
}
