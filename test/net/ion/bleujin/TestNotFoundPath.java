package net.ion.bleujin;

import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.niss.webapp.common.MyStaticFileHandler;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpHandler;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.handler.event.ServerEvent.EventType;
import net.ion.radon.aclient.NewClient;

public class TestNotFoundPath extends TestCase {

	public void testNotMapped() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(9900)
			.add("/found", new HttpHandler() {
				@Override
				public int order() {
					return 0;
				}
				
				@Override
				public void onEvent(EventType eventtype, Radon radon) {
					
				}
				
				@Override
				public void handleHttpRequest(HttpRequest req, HttpResponse res, HttpControl con) throws Exception {
					res.status(200).content("hello").end() ;
				}
			})
			.add(new MyStaticFileHandler("./webapps/admin/", Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("static-io-thread-%d"))))
			.add(new HttpHandler() {
				
				@Override
				public int order() {
					return 100;
				}
				
				@Override
				public void onEvent(EventType eventtype, Radon radon) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void handleHttpRequest(HttpRequest req, HttpResponse res, HttpControl con) throws Exception {
					res.status(404).content("not found " + res.status()).end() ;
				}
			}).startRadon() ;
		
		NewClient nc = NewClient.create() ;
		assertEquals("hello",  nc.prepareGet("http://localhost:9900/found").execute().get().getTextBody());
		assertEquals("dummy", nc.prepareGet("http://localhost:9900/dummy.php").execute().get().getTextBody()) ;
		assertEquals("not found 404", nc.prepareGet("http://localhost:9900/found/dummy.php1").execute().get().getTextBody()) ;
//		assertEquals("not found",  nc.prepareGet("http://localhost:9900/dummy.php").execute().get().getTextBody());
		
		nc.close(); 
		
		radon.stop().get() ;
		
	}
}
