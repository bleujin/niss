package net.ion.niss.webapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.resteasy.spi.HttpRequest;

import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Request;
import net.ion.radon.aclient.RequestBuilder;
import net.ion.radon.aclient.Response;
import net.ion.radon.core.let.PathHandler;
import junit.framework.TestCase;

public class TestEntity extends TestCase{

	
	public void testBodyEntity() throws Exception {
		
		Radon radon = RadonConfiguration.newBuilder(9500).add(new PathHandler(ResourceLet.class)) .startRadon() ;
		
		NewClient nc = NewClient.create() ;
		Request request = new RequestBuilder().setMethod(HttpMethod.GET).setUrl("http://localhost:9500/resource?name=bleujin").build() ;
		
		Response response = nc.prepareRequest(request).execute().get() ;
		Debug.line(response.getTextBody());
		
		nc.close(); 
		radon.stop().get() ;
	}
}

@Path("")
class ResourceLet {
	
	@QueryParam("name") protected String qname ;
	
	@GET
	@Path("/resource")
	public String hello(@Context HttpRequest request, @QueryParam("name") String name) throws IOException{
		InputStream input = request.getInputStream() ;
		Debug.line(IOUtil.toStringWithClose(input), name);
		
		return "hello" ;
	}
}
