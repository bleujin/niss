package net.ion.niss.config;

import java.util.Set;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.Debug;
import net.ion.niss.NissServer;
import net.ion.niss.config.builder.ConfigBuilder;
import net.ion.niss.webapp.REntry;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.DefaultCacheManager;

public class TestNSConfig extends TestCase {

	
	public void testConfirmDist() throws Exception {

		NSConfig nsConfig = ConfigBuilder.createDefault(9000).build() ;

		RepositoryConfig repoConfig = nsConfig.repoConfig() ;
		RepositoryImpl r = RepositoryImpl.create(new DefaultCacheManager(repoConfig.crakenConfig()), "niss");
		r.createWorkspace("admin", WorkspaceConfigBuilder.directory(repoConfig.adminHomeDir()));
		r.start();
		
		ReadSession session = r.login("admin");
		
		
		session.ghostBy("/indexers").children().debugPrint(); 
		
		Configuration config = r.dm().getCacheConfiguration("newind1-meta") ;
		Debug.line(config, r.dm().getDefaultCacheConfiguration().clustering());
		
		
		r.shutdown() ;
	}
	
	public void testLoad() throws Exception {
		RepositoryImpl r = RepositoryImpl.create(new DefaultCacheManager("./resource/config/craken-dist-config.xml"), "niss") ;
		r.defineWorkspace("search") ;
		ReadSession session = r.login("search") ;
		
		
		ReadNode node = session.pathBy("/") ;
		Set<PropertyId> keys = node.keys() ;
		
		assertEquals(0, keys.size()) ;
		session.workspace().repository().shutdown() ;
	}
	
	public void testDistMode() throws Exception {
		NSConfig nsconfig = ConfigBuilder.create("./resource/config/niss-dist-config.xml").build() ;
		NissServer ns = NissServer.create(nsconfig).start() ;
		REntry entry = ns.rentry() ;
		
		DefaultCacheManager dm = entry.repository().dm() ;
		
		assertEquals(CacheMode.DIST_SYNC, dm.getCache("admin").getCacheConfiguration().clustering().cacheMode());
		assertEquals(CacheMode.DIST_SYNC, dm.getDefaultCacheConfiguration().clustering().cacheMode());
		
		ns.shutdown() ;
	}
}
