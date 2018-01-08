package net.ion.niss.webapp.searchers;

import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import net.bleujin.rcraken.ReadSession;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.radon.core.ContextParam;

@Path("/templates")
public class TemplateWeb  implements Webapp{

	private ReadSession rsession ;
	public TemplateWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}

	// template
	@GET
	@Path("/{tid}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String viewTemplate(@PathParam("tid") final String tid){
		return rsession.pathBy("/templates/" + tid ).property("content").asString() ;
	}
	
	@POST
	@Path("/{tid}") 
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String editTemplate(@PathParam("tid") final String tid, @DefaultValue("") @FormParam("content") final String content) throws Exception{
		rsession.tran(wsession -> {
			wsession.pathBy("/templates/" + tid).property("content", content).merge();
		}) ;
		
		return "edit template" ;
	}

	
}
