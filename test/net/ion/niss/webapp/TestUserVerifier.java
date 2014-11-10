package net.ion.niss.webapp;

import org.infinispan.manager.DefaultCacheManager;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import junit.framework.TestCase;

public class TestUserVerifier extends TestCase {

	private ReadSession session;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		RepositoryImpl r = RepositoryImpl.create(new DefaultCacheManager("./resource/config/craken-local-config.xml"), "emanon") ;
		r.createWorkspace("test", WorkspaceConfigBuilder.directory("./resource/store/temp")) ;
		
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown() ;
		super.tearDown();
	}
	
	public void testJson() throws Exception {
		UserVerifier.test(session) ;
	}
	
	
	public void testWrite() throws Exception {
		session.ghostBy("/users/admin").debugPrint(); 
		
	}
	
}
