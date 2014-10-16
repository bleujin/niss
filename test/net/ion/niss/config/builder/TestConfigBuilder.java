package net.ion.niss.config.builder;

import net.ion.niss.config.NSConfig;
import junit.framework.TestCase;

public class TestConfigBuilder extends TestCase{

	public void testParse() throws Exception {
		NSConfig nsconfig = ConfigBuilder.create("./resource/config/niss-config.xml").build() ;
		
		assertEquals("niss", nsconfig.serverConfig().id());
		assertEquals(9000, nsconfig.serverConfig().port());

		assertEquals("./resource/index/", nsconfig.repoConfig().indexHomeDir());
		assertEquals("./resource/admin/", nsconfig.repoConfig().adminHomeDir());

	}
}
