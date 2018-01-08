package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.bleujin.rcraken.ReadSession;
import net.ion.niss.webapp.REntry;
import net.ion.radon.client.StubServer;

public class TestLoginWeb extends TestCase {
	

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(MiscWeb.class) ;
		REntry rentry = ss.treeContext().putAttribute(REntry.EntryName, REntry.test()) ;
		
		ReadSession session = rentry.login() ;
		session.tran(wsession -> {
			wsession.pathBy("/users/bleujin@i-on.net").property("password", "1").merge();
			return null;
		}) ;
	}
	
	public void testLogin() throws Exception {
		
		
	}

}
