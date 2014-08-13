package net.ion.niss.webapp.loader;

import net.ion.niss.webapp.REntry;
import net.ion.radon.client.StubServer;
import junit.framework.TestCase;

public class TestLoaderListener extends TestCase{

	private StubServer ss;


	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(LoaderWeb.class) ;
		ss.treeContext().putAttribute(REntry.EntryName, REntry.test()) ;

	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}
	
	
	public void testCreateScript() throws Exception {
		
	}
}
