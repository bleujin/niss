package net.ion.nradon.handler.authentication;

import junit.framework.TestCase;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

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
