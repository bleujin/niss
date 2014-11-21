package net.ion.bleujin;

import junit.framework.TestCase;
import net.ion.framework.util.InfinityThread;
import net.ion.niss.webapp.AppLogSink;
import net.ion.nradon.Radon;
import net.ion.nradon.authentication.WhoAmIHttpHandler;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.nradon.handler.authentication.BasicAuthenticationHandler;
import net.ion.nradon.handler.authentication.InMemoryPasswords;
import net.ion.nradon.handler.logging.LoggingHandler;

public class TestLogin extends TestCase {
	
	public void testRun() throws Exception {

		RadonConfigurationBuilder builder = RadonConfiguration.newBuilder(10000);

        InMemoryPasswords passwords = new InMemoryPasswords()
	        .add("bleujin", "1")
	        .add("hero", "1");

		builder.add(new LoggingHandler(new AppLogSink()))
		        .add(new BasicAuthenticationHandler(passwords))
//		        .add("/img/*", new BasicAuthenticationHandler(passwords))
				.add("/whoami", new WhoAmIHttpHandler())
				.add(new SimpleStaticFileHandler("./webapps/admin/")) ;

		Radon radon = builder.start().get();

		new InfinityThread().startNJoin();
	}
}
