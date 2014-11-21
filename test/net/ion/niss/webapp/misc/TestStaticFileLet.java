package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.aclient.FluentCaseInsensitiveStringsMap;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Response;
import net.ion.radon.client.StubServer;
import net.ion.radon.core.let.PathHandler;

import org.jboss.resteasy.util.HttpHeaderNames;

public class TestStaticFileLet extends TestCase{

	
	public void testSimpleGet() throws Exception {
		
		
		StubServer ss = StubServer.create(StaticFileLet.class) ;
		ss.treeContext().putAttribute("staticHome", "./webapps/admin") ;
		
		
		StubHttpResponse response = ss.request("/index.html").get() ;
		Debug.line(response.header(HttpHeaderNames.CONTENT_TYPE)) ;
	}
	
	
	public void testHttp() throws Exception {
		
		Radon radon = RadonConfiguration.newBuilder(8700).add(new PathHandler(StaticFileLet.class)).startRadon() ;
		radon.getConfig().getServiceContext().putAttribute("staticHome", "./webapps/admin") ;
		
		NewClient nc = NewClient.create() ;
		Response response = nc.prepareGet("http://localhost:8700/index.html").execute().get() ;

		FluentCaseInsensitiveStringsMap headers = response.getHeaders() ;
		
		for(String hname : headers.keySet()){
			Debug.line(hname, headers.getFirstValue(hname));
		}
		
		nc.close(); 
		radon.stop().get() ;
		
	}
}
