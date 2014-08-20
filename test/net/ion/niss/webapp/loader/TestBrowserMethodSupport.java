package net.ion.niss.webapp.loader;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.ion.framework.util.InfinityThread;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.radon.core.let.PathHandler;
import junit.framework.TestCase;

public class TestBrowserMethodSupport extends TestCase {


	public void testRun() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(9500)
			.add(new PathHandler(MethodLet.class)).startRadon() ;
		
		new InfinityThread().startNJoin(); 
	}
	
}


@Path("/methods")
class MethodLet {
	
	@PUT
	@Path("/{mid}")
	public String put(@PathParam("mid") String mid) {
		return mid ;
	}
	
	@GET
	@Path("/{mid}")
	public String get(@PathParam("mid") String mid) {
		return mid ;
	}
	
	@POST
	@Path("/{mid}")
	public String post(@PathParam("mid") String mid) {
		return mid ;
	}
	
	@DELETE
	@Path("/{mid}")
	public String delete(@PathParam("mid") String mid) {
		return mid ;
	}
}