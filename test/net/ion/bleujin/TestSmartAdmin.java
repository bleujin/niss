package net.ion.bleujin;

import junit.framework.TestCase;
import net.ion.framework.util.InfinityThread;
import net.ion.niss.apps.AppLogSink;
import net.ion.niss.apps.old.IndexManager;
import net.ion.niss.webapp.AnalysisWeb;
import net.ion.niss.webapp.MenuWeb;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.TemplateWeb;
import net.ion.niss.webapp.TunnelWeb;
import net.ion.niss.webapp.collection.CollectionWeb;
import net.ion.niss.webapp.loader.LoaderWeb;
import net.ion.niss.webapp.misc.MiscWeb;
import net.ion.niss.webapp.section.SectionWeb;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.nradon.handler.logging.LoggingHandler;
import net.ion.radon.core.let.PathHandler;

public class TestSmartAdmin extends TestCase {

	
	public void testRun() throws Exception {
		
		RadonConfigurationBuilder builder = RadonConfiguration.newBuilder(9000) ;
		builder.rootContext(REntry.EntryName, REntry.test()) ;
		
		builder
			.add(new LoggingHandler(new AppLogSink()))
			.add(new SimpleStaticFileHandler("./webapps/admin/"))
//			.add(new WhoAmIHttpHandler())
			.add("/admin/*", new PathHandler(LoaderWeb.class, CollectionWeb.class, SectionWeb.class, MiscWeb.class, MenuWeb.class, TemplateWeb.class, AnalysisWeb.class, TunnelWeb.class).prefixURI("/admin"))
			;
		
		Radon radon = builder.start().get() ;
		
		new InfinityThread().startNJoin(); 
		
	}
}
