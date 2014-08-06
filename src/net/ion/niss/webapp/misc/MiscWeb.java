package net.ion.niss.webapp.misc;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.apps.misc.PropertyInfo;
import net.ion.niss.apps.misc.ThreadDumpInfo;
import net.ion.niss.webapp.Webapp;

@Path("/misc")
public class MiscWeb implements Webapp{

	
	@GET
	@Path("/thread")
	public JsonObject listThreadDump() throws IOException{
		return new ThreadDumpInfo().list() ;
	}
	
	
	@GET
	@Path("/properties")
	public JsonObject listProperties(){
		return new PropertyInfo().list() ;
				
	}
}
