package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
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
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/users/bleujin@i-on.net").property("password", "1") ;
				return null;
			}
		}) ;
	}
	
	public void testLogin() throws Exception {
		
		
	}

}
