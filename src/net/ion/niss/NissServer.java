package net.ion.niss;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ion.craken.node.ReadSession;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.framework.util.InfinityThread;
import net.ion.niss.webapp.AppLogSink;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.HTMLTemplateEngine;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.common.MyStaticFileHandler;
import net.ion.niss.webapp.common.TraceHandler;
import net.ion.niss.webapp.common.MyAuthenticationHandler;
import net.ion.niss.webapp.indexers.IndexerWeb;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.LoaderWeb;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.niss.webapp.misc.CrakenLet;
import net.ion.niss.webapp.misc.MenuWeb;
import net.ion.niss.webapp.misc.MiscWeb;
import net.ion.niss.webapp.misc.TunnelWeb;
import net.ion.niss.webapp.searchers.OpenSearchWeb;
import net.ion.niss.webapp.searchers.SearcherWeb;
import net.ion.niss.webapp.searchers.TemplateWeb;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceHandler;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.nradon.handler.StaticFile;
import net.ion.nradon.handler.TemplateEngine;
import net.ion.nradon.handler.authentication.BasicAuthenticationHandler;
import net.ion.nradon.handler.authentication.InMemoryPasswords;
import net.ion.nradon.handler.authentication.SessionAuthenticationHandler;
import net.ion.nradon.handler.logging.LoggingHandler;
import net.ion.radon.core.let.PathHandler;

public class NissServer {

	private RadonConfigurationBuilder builder;
	private Radon radon;
	private int portNum = 9000;

	public NissServer(int portNum){
		this.portNum = portNum ;
	}

	public static NissServer create() throws IOException, InterruptedException, ExecutionException{
		return create(9000) ;
	}

	public static NissServer create(int portNum) throws IOException, InterruptedException, ExecutionException{
		NissServer server = new NissServer(portNum) ;
		server.init(); 
		return server ;
	}
	
	public void init() throws IOException {
		this.builder = RadonConfiguration.newBuilder(portNum);

		final REntry rentry = builder.context(REntry.EntryName, REntry.create());
		final EventSourceEntry esentry = builder.context(EventSourceEntry.EntryName, EventSourceEntry.create());
		final JScriptEngine jsentry = builder.context(JScriptEngine.EntryName, JScriptEngine.create());
		jsentry.executorService(Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("jscript-thread-%d")));


		this.radon = builder.createRadon() ;

		radon
			.add(new MyAuthenticationHandler(new InMemoryPasswords().add("bleujin", "1")))
			.add("/admin/*", new TraceHandler(rentry))
			.add(new LoggingHandler(new AppLogSink()))
			.add(new MyStaticFileHandler("./webapps/admin/", Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("static-io-thread-%d")), new HTMLTemplateEngine(radon.getConfig().getServiceContext())))
			// .add(new WhoAmIHttpHandler())
			.add("/admin/*", new PathHandler(LoaderWeb.class, IndexerWeb.class, SearcherWeb.class, MiscWeb.class, MenuWeb.class, CrakenLet.class, TemplateWeb.class, AnalysisWeb.class, TunnelWeb.class).prefixURI("/admin"))
			.add("/search/*", new PathHandler(OpenSearchWeb.class).prefixURI("search"))
			.add("/event/{id}", new EventSourceHandler() {
				@Override
				public void onOpen(EventSourceConnection conn) throws Exception {
					esentry.onOpen(conn);
				}

				@Override
				public void onClose(EventSourceConnection conn) throws Exception {
					esentry.onClose(conn);
				}
			});
		
		
	}
	
	public NissServer start() throws InterruptedException, ExecutionException, IOException{
		if (this.builder == null) init(); 
		
		this.radon.start().get() ;
		return this ;
	}

	public NissServer shutdown() throws InterruptedException, ExecutionException{
		radon.stop().get() ;
		return this ;
	}
	
	
	
	
}
