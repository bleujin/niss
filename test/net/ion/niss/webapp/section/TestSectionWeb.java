package net.ion.niss.webapp.section;

import net.ion.framework.util.Debug;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;
import junit.framework.TestCase;

public class TestSectionWeb extends TestCase{

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(SectionWeb.class) ;
	}
	
	
	public void testArray() throws Exception {
		StubHttpResponse response = ss.request("/sections/sec1").postParam("collection", "col1").postParam("collection", "col2").post() ;
		Debug.line(response.contentsString());
	}
}
