package net.ion.niss.webapp;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.niss.webapp.common.MyVerifier;

import org.infinispan.manager.DefaultCacheManager;

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
		MyVerifier.test(session) ;
	}
	
	
	public void testWrite() throws Exception {
		session.ghostBy("/users/admin").debugPrint(); 
		
	}
	
}
