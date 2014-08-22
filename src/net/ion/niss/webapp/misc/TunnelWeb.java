package net.ion.niss.webapp.misc;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Functions;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.webapp.REntry;
import net.ion.radon.core.ContextParam;

import org.jboss.resteasy.spi.HttpRequest;

@Path("/tunnel")
public class TunnelWeb {

	private ReadSession rsession;
	public TunnelWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}
	
	@POST
	@Path("{fqn : .*}")
	public String editField(@PathParam("fqn") final String fqn, @Context final HttpRequest request){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode wnode = wsession.pathBy("/" + fqn) ;
				MultivaluedMap<String, String> params = request.getFormParameters() ;
				for(String key : params.keySet()){
					if (params.get(key).size() == 1) wnode.property(key, params.getFirst(key)) ;
					else wnode.append(key, params.get(key).toArray(new String[0])) ;
				}
				return null;
			}
		}) ;
		
		return fqn + " edited" ;
	}
	
	
	@GET
	@Path("{fqn : .*}")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewNode(@PathParam("fqn") String fqn){
		ReadNode node = rsession.pathBy("/" + fqn) ;
		return node.transformer(Functions.toJson()) ;
	}
}
