package net.ion.bleujin;

import net.ion.framework.util.InfinityThread;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import junit.framework.TestCase;

public class TestSmartAdmin extends TestCase {

	public void testRun() throws Exception {
		RadonConfigurationBuilder builder = RadonConfiguration.newBuilder(9000) ;
		builder.add(new SimpleStaticFileHandler("./webapps/admin/")) ;
		
		Radon radon = builder.start().get() ;
		
		new InfinityThread().startNJoin(); 
		
	}
}
