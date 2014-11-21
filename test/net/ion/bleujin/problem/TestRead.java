package net.ion.bleujin.problem;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;

import org.infinispan.manager.DefaultCacheManager;

public class TestRead extends TestCase {

	
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		RepositoryImpl r = RepositoryImpl.create(new DefaultCacheManager("./resource/config/craken-local-config.xml"), "niss") ;
		r.createWorkspace("admin", WorkspaceConfigBuilder.directory("./resource/admin")) ;
		
		this.session = r.login("admin") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown() ;
		super.tearDown();
	}
	
	public void testWrite() throws Exception {
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/users/bleujin").property("name", "bleujin") ;
				return null;
			}
		}).get() ;
		session.pathBy("/users").children().debugPrint(); 
	}

	public void testRead() throws Exception {
		session.pathBy("/users").children().debugPrint(); 
		
	}

}
