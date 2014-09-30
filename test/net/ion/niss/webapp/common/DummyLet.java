package net.ion.niss.webapp.common;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path("")
public class DummyLet {

	@GET
	@Path("{remain:.*}")
	public String get(){
		return "get called";
	}

	@POST
	@Path("{remain:.*}")
	public String post(){
		return "post called";
		
	}

	@DELETE
	@Path("{remain:.*}")
	public String delete(){
		return "delete called";
	}
	
	@PUT
	@Path("{remain:.*}")
	public String put(){
		return "put called";
	}
	
	
}

