package net.ion.niss.webapp;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.apps.CollectionApp;
import net.ion.radon.core.ContextParam;
import net.ion.radon.core.IService;

@Path("/collections")
public class CollectionWeb implements Webapp{

	private CollectionApp app ;
	public CollectionWeb(@ContextParam("CollectionApp") CollectionApp app){
		this.app = app ;
	}
	
	@GET
	@Path("/{cid}/status")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewStatus(@PathParam("cid") String cid) throws IOException{
		return app.find(cid).status() ;
		
	}
	
}
