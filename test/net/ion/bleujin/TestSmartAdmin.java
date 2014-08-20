package net.ion.bleujin;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ObjectId;
import net.ion.niss.apps.AppLogSink;
import net.ion.niss.apps.loader.JScriptEngine;
import net.ion.niss.apps.old.IndexManager;
import net.ion.niss.webapp.AnalysisWeb;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.MenuWeb;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.TunnelWeb;
import net.ion.niss.webapp.collection.CollectionWeb;
import net.ion.niss.webapp.loader.LoaderWeb;
import net.ion.niss.webapp.misc.MiscWeb;
import net.ion.niss.webapp.section.SectionWeb;
import net.ion.niss.webapp.section.TemplateWeb;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceHandler;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.AbstractHttpHandler;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.nradon.handler.event.ServerEvent.EventType;
import net.ion.nradon.handler.logging.LoggingHandler;
import net.ion.radon.core.let.PathHandler;

public class TestSmartAdmin extends TestCase {

	
	public void testRun() throws Exception {
		
		RadonConfigurationBuilder builder = RadonConfiguration.newBuilder(9000) ;
		
		final REntry rentry = builder.context(REntry.EntryName, REntry.test()) ;
		final EventSourceEntry esentry = builder.context(EventSourceEntry.EntryName, EventSourceEntry.create()) ;
		final JScriptEngine jsentry = builder.context(JScriptEngine.EntryName, JScriptEngine.create()) ;
		jsentry.executorService(Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("jscript-thread-%d"))) ;
		
		ReadSession rsession = rentry.login() ;
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				int i = 100 ;
				for (File file : new File("./resource/loader").listFiles()) {
					wsession.pathBy("/loaders/" + i++).property("name", file.getName()).property("content", IOUtil.toStringWithClose(new FileInputStream(file))) ;	
				}
				return null;
			}
		}) ;

		
		
		builder
			.add(new LoggingHandler(new AppLogSink()))
			.add(new SimpleStaticFileHandler("./webapps/admin/"))
//			.add(new WhoAmIHttpHandler())
			.add("/admin/*", new PathHandler(LoaderWeb.class, CollectionWeb.class, SectionWeb.class, MiscWeb.class, MenuWeb.class, TemplateWeb.class, AnalysisWeb.class, TunnelWeb.class).prefixURI("/admin"))
			.add("/event/{id}", new EventSourceHandler() {
				@Override
				public void onOpen(EventSourceConnection conn) throws Exception {
					esentry.onOpen(conn) ;
				}
				@Override
				public void onClose(EventSourceConnection conn) throws Exception {
					esentry.onClose(conn) ;
				}
			})
			.add("/emonitor", new ESHtmlHandler())
			;
		
		Radon radon = builder.start().get() ;
		
		new InfinityThread().startNJoin(); 
		
	}
	
	private static class ESHtmlHandler extends AbstractHttpHandler {
		public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
			response.header("Content-Type", "text/html").charset(Charset.forName("UTF-8")).content(
					"" + "<!DOCTYPE html>\n" + "<html>\n" 
					+ "  <head>\n" 
					+ "    <script>\n" 
					+ "      function logText(msg) {\n" + "        var textArea = document.getElementById('log');\n" 
					+ "        textArea.value = textArea.value + msg + '\\n';\n"
					+ "        textArea.scrollTop = textArea.scrollHeight; // scroll into view\n" 
					+ "      }\n\n" 
					+ "      window.onload = function() {\n" 
					+ "        var es = new EventSource('/event/1111');\n" 
					+ "        es.onopen = function() {\n"
					+ "          console.log('OPEN');\n" 
					+ "        };\n" 
					+ "        es.onmessage = function(e) {\n" 
					+ "          logText(e.data);\n" 
					+ "        };\n" 
					+ "        es.onerror = function(e) {\n" 
					+ "          console.log('ERROR');\n" 
					+ "        };\n" 
					+ "      };\n"
					+ "    </script>\n" + "  </head>\n" 
					+ "  <body>\n" 
					+ "    <textarea id=\"log\" rows=\"40\" cols=\"70\"></textarea>\n" + "  </body>\n" + "</html>")
					.end();
		}

		public void onEvent(EventType event, Radon wserver) {

		}
	}
}


