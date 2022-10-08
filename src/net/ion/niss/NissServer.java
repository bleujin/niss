package net.ion.niss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.lucene.analysis.Analyzer;

import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.framework.util.ListUtil;
import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;
import net.ion.niss.webapp.AppLogSink;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.HTMLTemplateEngine;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.MyAppCacheHandler;
import net.ion.niss.webapp.common.MyAuthenticationHandler;
import net.ion.niss.webapp.common.MyEventLog;
import net.ion.niss.webapp.common.MyStaticFileHandler;
import net.ion.niss.webapp.common.MyVerifier;
import net.ion.niss.webapp.common.TraceHandler;
import net.ion.niss.webapp.dscripts.DScriptWeb;
import net.ion.niss.webapp.indexers.IndexerWeb;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.LoaderWeb;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.niss.webapp.misc.CrakenLet;
import net.ion.niss.webapp.misc.ExportWeb;
import net.ion.niss.webapp.misc.MenuWeb;
import net.ion.niss.webapp.misc.MiscWeb;
import net.ion.niss.webapp.misc.OpenDScriptWeb;
import net.ion.niss.webapp.misc.OpenScriptWeb;
import net.ion.niss.webapp.misc.TraceWeb;
import net.ion.niss.webapp.misc.TunnelWeb;
import net.ion.niss.webapp.scripters.ScriptWeb;
import net.ion.niss.webapp.searchers.OpenSearchWeb;
import net.ion.niss.webapp.searchers.PopularQueryEntry;
import net.ion.niss.webapp.searchers.QueryTemplateEngine;
import net.ion.niss.webapp.searchers.SearcherWeb;
import net.ion.niss.webapp.searchers.TemplateWeb;
import net.ion.niss.webapp.sites.SiteWeb;
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

	private enum Status {
		STOPED, INITED, STARTED;
	}

	private AtomicReference<Status> status = new AtomicReference<NissServer.Status>(Status.STOPED);
	private REntry rentry;

	NissServer(NSConfig nsconfig) {
		this.nsconfig = nsconfig;
	}

	public static NissServer create(NSConfig nsconfig) throws Exception {
		NissServer server = new NissServer(nsconfig);
		server.init();
		return server;
	}

	public static NissServer create(int portNum) throws Exception {
		return create(ConfigBuilder.createDefault(portNum).build());
	}

	public void init() throws Exception {
		this.builder = RadonConfiguration.newBuilder(nsconfig.serverConfig().port());

		this.rentry = builder.context(REntry.EntryName, REntry.create(nsconfig));

		final EventSourceEntry esentry = builder.context(EventSourceEntry.EntryName, EventSourceEntry.create());
		final JScriptEngine jsentry = builder.context(JScriptEngine.EntryName, JScriptEngine.create("./resource/loader/lib", Executors.newSingleThreadScheduledExecutor(ThreadFactoryBuilder.createThreadFactory("script-monitor-thread-%d")), true));
		jsentry.executorService(Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("jscript-thread-%d")));

		ExecutorService nworker = Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("nworker-thread-%d")) ;
		builder.context(PopularQueryEntry.EntryName, new PopularQueryEntry(rentry.login(), nworker)) ;
		
		final QueryTemplateEngine ve = builder.context(QueryTemplateEngine.EntryName, QueryTemplateEngine.create("my.craken", rentry.login()));

		this.radon = builder.createRadon();

		final MyEventLog elogger = MyEventLog.create(System.out);
		radon.add(new MyAuthenticationHandler(MyVerifier.test(rentry.login())))
				.add("/admin/*", new TraceHandler(rentry))
//				.add("/favicon.ico", new FavIconHandler())
				.add(new MyAppCacheHandler("./webapps/admin/cache.appcache"))
				.add(new LoggingHandler(new AppLogSink(elogger)))
				.add(new MyStaticFileHandler("./webapps/admin/", Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("static-io-thread-%d")), new HTMLTemplateEngine(radon.getConfig().getServiceContext())).welcomeFile("index.html"))
				// .add(new WhoAmIHttpHandler())
				.add("/admin/*", new PathHandler(LoaderWeb.class, IndexerWeb.class, SearcherWeb.class, SiteWeb.class,  MiscWeb.class, ScriptWeb.class, DScriptWeb.class, MenuWeb.class, CrakenLet.class, TemplateWeb.class, AnalysisWeb.class, TraceWeb.class, TunnelWeb.class, ExportWeb.class).prefixURI("/admin"))
				.add("/open/*", new PathHandler(OpenSearchWeb.class, OpenScriptWeb.class, OpenDScriptWeb.class).prefixURI("open"))
				.add("/logging/event/*", new EventSourceHandler() {
					@Override
					public void onOpen(EventSourceConnection econn) throws Exception {
						elogger.onOpen(econn);
					}

					@Override
					public void onClose(EventSourceConnection econn) throws Exception {
						elogger.onClose(econn);
					}
				}).add("/event/{id}", new EventSourceHandler() {
					@Override
					public void onOpen(EventSourceConnection conn) throws Exception {
						esentry.onOpen(conn);
					}

					@Override
					public void onClose(EventSourceConnection conn) throws Exception {
						esentry.onClose(conn);
					}
				}).add(new HttpHandler() {

					@Override
					public void onEvent(EventType eventtype, Radon radon) {
					}

					@Override
					public int order() {
						return 1000;
					}

					@Override
					public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
						response.status(404).content("not found path : " + request.uri()).end();
					}
				});

		File loadanal = new File(Webapp.ANALYSIS_FILE);
		if (loadanal.exists() && loadanal.isFile()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Webapp.ANALYSIS_FILE), "UTF-8"));
			String line = null;
			List<Class<? extends Analyzer>> loaded = ListUtil.newList();
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("//"))
					continue;
				try {
					loaded.add((Class<? extends Analyzer>) Class.forName(line));
				} catch (Exception ignore) {
					System.err.println(ignore.getMessage());
				}
			}
			AnalysisWeb.appendAnalyzer(loaded);
		}

		radon.getConfig().getServiceContext().putAttribute(NissServer.class.getCanonicalName(), this);
		status.set(Status.INITED);
		;
	}

	public NSConfig config() {
		return nsconfig;
	}

	@Deprecated
	// only test
	public REntry rentry() {
		return rentry;
	}

	public NissServer start() throws Exception {
		if (this.builder == null)
			init();

		this.radon.start().get();
		status.set(Status.STARTED);
		return this;
	}

	public NissServer shutdown() throws InterruptedException, ExecutionException {
		if (status.get() == Status.STOPED)
			return this;

		radon.stop().get();
		status.set(Status.STOPED);
		return this;
	}

}
