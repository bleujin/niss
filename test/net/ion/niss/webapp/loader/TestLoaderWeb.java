package net.ion.niss.webapp.loader;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.niss.apps.Store;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestLoaderWeb extends TestCase{

	private StubServer ss;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(LoaderWeb.class) ;
		ss.treeContext().putAttribute(Store.class.getSimpleName(), Store.test()) ;
	}
	
	public void testScriptList() throws Exception {
		StubHttpResponse response = ss.request("/loaders").get() ;
		assertEquals(200, response.status());
		Debug.line(response.contentsString());
	}
}
