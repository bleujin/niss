package net.ion.niss.config.builder;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.ion.framework.util.Debug;
import net.ion.niss.config.NSConfig;

public class TestConfigBuilder extends TestCase{

	public void testParse() throws Exception {
		NSConfig nsconfig = ConfigBuilder.create("./resource/config/niss-config.xml").build() ;
		
		assertEquals("niss", nsconfig.serverConfig().id());
		assertEquals(9000, nsconfig.serverConfig().port());

		assertEquals("./resource/index/", nsconfig.repoConfig().indexHomeDir());
		assertEquals("./resource/admin/", nsconfig.repoConfig().adminHomeDir());

		Debug.debug("./resource/admin/", nsconfig.repoConfig().craken());
		
		Craken c = nsconfig.repoConfig().craken().start() ;
		c.shutdown() ;
		
	}
}
