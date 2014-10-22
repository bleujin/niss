package net.ion.niss;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;
import net.ion.niss.webapp.AppLogSink;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.HTMLTemplateEngine;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.UserVerifier;
import net.ion.niss.webapp.common.FavIconHandler;
import net.ion.niss.webapp.common.MyAuthenticationHandler;
import net.ion.niss.webapp.common.MyEventLog;
import net.ion.niss.webapp.common.MyStaticFileHandler;
import net.ion.niss.webapp.common.TraceHandler;
import net.ion.niss.webapp.indexers.IndexerWeb;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.LoaderWeb;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.niss.webapp.misc.CrakenLet;
import net.ion.niss.webapp.misc.MenuWeb;
import net.ion.niss.webapp.misc.MiscWeb;
import net.ion.niss.webapp.misc.TraceWeb;
import net.ion.niss.webapp.misc.TunnelWeb;
import net.ion.niss.webapp.searchers.OpenSearchWeb;
import net.ion.niss.webapp.searchers.QueryTemplateEngine;
import net.ion.niss.webapp.searchers.SearcherWeb;
import net.ion.niss.webapp.searchers.TemplateWeb;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceHandler;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpHandler;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.event.ServerEvent.EventType;
import net.ion.nradon.handler.logging.LoggingHandler;
import net.ion.radon.core.let.PathHandler;

public class NissServer {

	private RadonConfigurationBuilder builder;
	private Radon radon;
	private NSConfig nsconfig;
	
	private enum Status{
		STOPED, INITED, STARTED ;
	}
	
	private AtomicReference<Status> status =  new AtomicReference<NissServer.Status>(Status.STOPED) ;

	NissServer(NSConfig nsconfig){
		this.nsconfig = nsconfig ;
	}

	public static NissServer create(NSConfig nsconfig) throws IOException, InterruptedException, ExecutionException{
		NissServer server = new NissServer(nsconfig) ;
		server.init(); 
		return server ;
	}

	public static NissServer create(int portNum) throws IOException, InterruptedException, ExecutionException{
		 return create(ConfigBuilder.createDefault(portNum).build());
	}
	
	public void init() throws IOException {
		this.builder = RadonConfiguration.newBuilder(nsconfig.serverConfig().port());

		final REntry rentry = builder.context(REntry.EntryName, REntry.create(nsconfig));
		final EventSourceEntry esentry = builder.context(EventSourceEntry.EntryName, EventSourceEntry.create());
		final JScriptEngine jsentry = builder.context(JScriptEngine.EntryName, JScriptEngine.create());
		jsentry.executorService(Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("jscript-thread-%d")));


		final QueryTemplateEngine ve = builder.context(QueryTemplateEngine.EntryName, QueryTemplateEngine.create("my.craken", rentry.login()));
		
		this.radon = builder.createRadon() ;

		final MyEventLog elogger = MyEventLog.create(System.out);
		radon
			.add(new MyAuthenticationHandler(UserVerifier.test(rentry.login())))
			.add("/admin/*", new TraceHandler(rentry))
			.add("/favicon.ico", new FavIconHandler())
			.add(new LoggingHandler(new AppLogSink(elogger)))
			.add(new MyStaticFileHandler("./webapps/admin/", Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("static-io-thread-%d")), new HTMLTemplateEngine(radon.getConfig().getServiceContext())).welcomeFile("index.html") )
			// .add(new WhoAmIHttpHandler())
			.add("/admin/*", new PathHandler(LoaderWeb.class, IndexerWeb.class, SearcherWeb.class, MiscWeb.class, MenuWeb.class, CrakenLet.class, TemplateWeb.class, AnalysisWeb.class, TraceWeb.class, TunnelWeb.class).prefixURI("/admin"))
			.add("/search/*", new PathHandler(OpenSearchWeb.class).prefixURI("search"))
			.add("/logging/event/*", new EventSourceHandler(){
				@Override
				public void onOpen(EventSourceConnection econn) throws Exception {
					elogger.onOpen(econn) ;
				}
				@Override
				public void onClose(EventSourceConnection econn) throws Exception {
					elogger.onClose(econn) ;
				}
			})
			.add("/event/{id}", new EventSourceHandler() {
				@Override
				public void onOpen(EventSourceConnection conn) throws Exception {
					esentry.onOpen(conn);
				}

				@Override
				public void onClose(EventSourceConnection conn) throws Exception {
					esentry.onClose(conn);
				}
			}).add(new HttpHandler(){

				@Override
				public void onEvent(EventType eventtype, Radon radon) {
				}

				@Override
				public int order() {
					return 1000;
				}

				@Override
				public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
					response.status(404).content("not found path : " + request.uri()).end() ;
				}
			});
		
		radon.getConfig().getServiceContext().putAttribute(NissServer.class.getCanonicalName(), this) ;
		status.set(Status.INITED); ;
	}
	
	public NSConfig config(){
		return nsconfig ;
	}
	
	public NissServer start() throws InterruptedException, ExecutionException, IOException{
		if (this.builder == null) init(); 
		
		this.radon.start().get() ;
		status.set(Status.STARTED);
		return this ;
	}

	public NissServer shutdown() throws InterruptedException, ExecutionException{
		if (status.get() == Status.STOPED) return this ;
		
		radon.stop().get() ;
		status.set(Status.STOPED);
		return this ;
	}
	
	
	
}
