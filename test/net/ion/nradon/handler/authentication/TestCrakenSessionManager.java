package net.ion.nradon.handler.authentication;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;

public class TestCrakenSessionManager extends TestCase{

	
	public void testSession() throws Exception {
//		DefaultCacheManager dcm = new DefaultCacheManager(new GlobalConfigurationBuilder().transport().clusterName("test_session").build()) ;
//		dcm.defineConfiguration("dsession", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).build()) ;

		DefaultCacheManager dcm = new DefaultCacheManager() ;
		
		Cache<String, SessionInfo> cache = dcm.getCache("dsession") ;
		CrakenSessionManager csm = new CrakenSessionManager(cache) ;
		csm.liveSeconds(1) ;
		
		assertEquals(false, csm.hasSession("bleujin")) ;
		
		SessionInfo sinfo = csm.newSession("bleujin") ;
		sinfo.register("name", "myName") ;
		
		assertEquals(true, csm.findSession("bleujin").hasValue("name")) ;
		assertEquals("bleujin", csm.findSession("bleujin").sessionKey()) ;
		
		assertEquals("myName", csm.findSession("bleujin").value("name")) ;
		assertEquals("blank", csm.findSession("bleujin").value("notfound", "blank")) ;

		Thread.sleep(1500); 
		assertEquals(false, csm.hasSession("bleujin")) ;
		dcm.stop(); 
	}
}
