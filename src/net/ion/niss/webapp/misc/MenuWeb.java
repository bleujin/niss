package net.ion.niss.webapp.misc;

import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bleujin.rcraken.ReadSession;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

@Path("/menus")
public class MenuWeb implements Webapp{

	
	private ReadSession rsession;

	public MenuWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}

	
	
	@POST
	@Path("/{menu : .*}")
	public String updateInfo(@PathParam("menu") final String menu, @DefaultValue("overview") @FormParam("field") final String field, @DefaultValue("") @FormParam("content") final String content){
		rsession.tran( wsession -> {
			wsession.pathBy("/menus/" + menu).property(field, content).merge();
		}) ;
		
		return "update info" ;
	}
	
	
	@GET
	@Path("/{menu}")
	public String viewInfo(@PathParam("menu") final String menu, @DefaultValue("overview") @QueryParam("field") final String field){
		return rsession.pathBy("/menus/" + menu).property(field).asString() ;
	}
}
