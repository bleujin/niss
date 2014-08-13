package net.ion.niss.webapp;

import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.radon.core.ContextParam;

@Path("/menus")
public class MenuWeb implements Webapp{

	
	private ReadSession rsession;

	public MenuWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}

	
	
	@POST
	@Path("/{menu}")
	public String updateInfo(@PathParam("menu") final String menu, @DefaultValue("overview") @FormParam("field") final String field, @DefaultValue("") @FormParam("content") final String content){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/menus/" + menu).property(field, content) ;
				return null;
			}
		}) ;
		
		return "update info" ;
	}
	
	
	@GET
	@Path("/{menu}")
	public String viewInfo(@PathParam("menu") final String menu, @DefaultValue("overview") @QueryParam("field") final String field){
		return rsession.pathBy("/menus/" + menu).property(field).asString() ;
	}
}
