package net.ion.bleujin;

import org.apache.commons.lang.SystemUtils;

import junit.framework.TestCase;
import net.ion.framework.util.InfinityThread;
import net.ion.niss.apps.AppLogSink;
import net.ion.niss.apps.collection.IndexCollectionApp;
import net.ion.niss.webapp.collection.OldCollectionWeb;
import net.ion.niss.webapp.misc.MiscWeb;
import net.ion.nradon.Radon;
import net.ion.nradon.authentication.WhoAmIHttpHandler;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.nradon.handler.logging.LoggingHandler;
import net.ion.nradon.handler.logging.SimpleLogSink;
import net.ion.radon.core.let.PathHandler;

public class TestSmartAdmin extends TestCase {

	public void testRun() throws Exception {
		
		RadonConfigurationBuilder builder = RadonConfiguration.newBuilder(9000) ;
		builder.rootContext(IndexCollectionApp.class.getSimpleName(), IndexCollectionApp.create()) ;
		
		builder
			.add(new LoggingHandler(new AppLogSink()))
			.add(new SimpleStaticFileHandler("./webapps/admin/"))
//			.add(new WhoAmIHttpHandler())
			.add("/admin/*", new PathHandler(OldCollectionWeb.class, MiscWeb.class).prefixURI("/admin"))
			;
		
		Radon radon = builder.start().get() ;
		
		new InfinityThread().startNJoin(); 
		
	}
}
