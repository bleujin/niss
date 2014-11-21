package net.ion.niss.webapp.loaders;

import junit.framework.TestCase;
import net.ion.niss.webapp.REntry;
import net.ion.radon.client.StubServer;

public class TestScriptRun extends TestCase{

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
