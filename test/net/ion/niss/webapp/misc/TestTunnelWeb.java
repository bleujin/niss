package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.radon.client.StubServer;

public class TestTunnelWeb extends TestCase {
	
	private StubServer ss;
	private REntry entry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(TunnelWeb.class) ;
		this.entry = ss.treeContext().putAttribute(REntry.EntryName, REntry.test()) ;
		entry.login().tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/users/bleujin").property("name", "bleujin").child("address").property("address", "seoul");
				return null;
			}
		}) ;
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.entry.close(); 
		super.tearDown();
	}
	
	public void testView() throws Exception {
		assertEquals(404, ss.request("/tunnel/notfound/bleujin").get().status());
		assertEquals(200, ss.request("/tunnel/users/bleujin").get().status() );

		Debug.debug(ss.request("/tunnel/users/bleujin").get().contentsString());
		Debug.debug(ss.request("/tunnel/users/bleujin.node").get().contentsString());
		Debug.debug(ss.request("/tunnel/users/bleujin.list").get().contentsString());
	}
	
	
}
