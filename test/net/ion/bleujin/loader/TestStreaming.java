package net.ion.bleujin.loader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import junit.framework.TestCase;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceHandler;
import net.ion.nradon.EventSourceMessage;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.handler.AbstractHttpHandler;
import net.ion.radon.core.let.PathHandler;

public class TestStreaming extends TestCase {

	public void testStreamingRun() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(9500)
					.add(new PathHandler(StreamLet.class))
					.startRadon() ;
		
		new InfinityThread().startNJoin(); 
	}
	
	public void testEventSource() throws Exception {
		final CopyOnWriteArraySet<EventSourceConnection> conns = new CopyOnWriteArraySet<EventSourceConnection>() ;
		Radon radon = RadonConfiguration.newBuilder(9500)
				.add("/html", new HtmlHandler())
				.add("/event/{id}", new EventSourceHandler() {
					@Override
					public void onOpen(EventSourceConnection conn) throws Exception {
						conns.add(conn) ;
					}
					
					@Override
					public void onClose(EventSourceConnection conn) throws Exception {
						conns.remove(conn) ;
					}
				}).startRadon() ;
		
		
		new Thread(){
			public void run(){
				while(1 > 0){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for (EventSourceConnection c : conns) {
						c.send(new EventSourceMessage("time : " + System.currentTimeMillis() + " to " + c.httpRequest().data("id") )) ;
					}
				}
			}
		}.start(); 
		
		
		new InfinityThread().startNJoin(); 
	}
	
	private static class HtmlHandler extends AbstractHttpHandler {
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
					+ "        var es = new EventSource('/event/bleujin');\n" 
					+ "        es.onopen = function() {\n"
					+ "          logText('OPEN');\n" 
					+ "        };\n" 
					+ "        es.onmessage = function(e) {\n" 
					+ "          logText('MESSAGE:' + e.data);\n" 
					+ "        };\n" 
					+ "        es.onerror = function(e) {\n" 
					+ "          logText('ERROR');\n" 
					+ "        };\n" 
					+ "      };\n"
					+ "    </script>\n" + "  </head>\n" 
					+ "  <body>\n" 
					+ "    <textarea id=\"log\" rows=\"40\" cols=\"70\"></textarea>\n" + "  </body>\n" + "</html>")
					.end();
		}
	}
}

@Path("/stream")
class StreamLet {
	
	@Path("")
	@GET
	public StreamingOutput process(){
		return new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				int i = 0 ;
				OutputStreamWriter writer = new OutputStreamWriter(output, Charset.forName("UTF-8")) ;
				while(i++ < 100){
					StringBuilder sb = new StringBuilder() ;
					for (int j = 0; j < 100; j++) {
						sb.append("Hello " + i) ;
					}
					writer.write(sb.toString());
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					output.flush();
					System.out.print('.');
				}
			}
		};
	}
	
	
}
