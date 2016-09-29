package net.ion.bleujin;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import junit.framework.TestCase;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.radon.core.let.PathHandler;

@Path("/hello")
public class TestResponse extends TestCase {
	
	public void testCreate() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(9000).start().get() ;
		
		radon.add("*", new PathHandler(getClass())) ;
		
		new InfinityThread().startNJoin(); 
	}
	
	@Path("/{name}")
	@GET
	public Response hello(@PathParam("name") String name){
		return Response.status(400).entity("hello " + name).build() ;
	}

	
	
}
