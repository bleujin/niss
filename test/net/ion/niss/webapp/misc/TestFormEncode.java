package net.ion.niss.webapp.misc;

import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.spi.HttpRequest;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.radon.aclient.ListenableFuture;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Response;
import net.ion.radon.core.let.PathHandler;

public class TestFormEncode extends TestCase {

	
	public void testConfirm() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(8999).add(new PathHandler(ViewForm.class)).start().get() ;
		
		NewClient nc = NewClient.create() ;
		ListenableFuture<Response> future = nc.preparePost("http://localhost:8999/view").addParameter("name", "park jin").execute() ;
		Debug.line(future.get().getTextBody());
		nc.close(); 
		
		radon.stop() ;
	}
}

@Path("")
class ViewForm {
	
	
	@Path("/view")
	@POST
	public String viewFormParam(@Context HttpRequest request){
		MultivaluedMap<String, String> params = request.getDecodedFormParameters() ;
		for(Entry<String, List<String>> entry : params.entrySet()){
			Debug.line(entry.getValue().get(0));
		}
		
		
		
		return "runned" ;
	}
	
}
