package net.ion.bleujin;

import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.framework.util.InfinityThread;
import net.ion.niss.webapp.AppLogSink;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.indexers.IndexerWeb;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.LoaderWeb;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.niss.webapp.misc.MenuWeb;
import net.ion.niss.webapp.misc.MiscWeb;
import net.ion.niss.webapp.misc.TunnelWeb;
import net.ion.niss.webapp.searchers.SearcherWeb;
import net.ion.niss.webapp.searchers.TemplateWeb;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceHandler;
import net.ion.nradon.Radon;
import net.ion.nradon.authentication.WhoAmIHttpHandler;
import net.ion.nradon.authentication.WhoAmIWebSocketHandler;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.nradon.handler.authentication.BasicAuthenticationHandler;
import net.ion.nradon.handler.authentication.InMemoryPasswords;
import net.ion.nradon.handler.logging.LoggingHandler;
import net.ion.radon.core.let.PathHandler;

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
