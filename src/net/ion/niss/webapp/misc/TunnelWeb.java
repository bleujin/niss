package net.ion.niss.webapp.misc;

import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.WriteNode;
import net.bleujin.rcraken.convert.Functions;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.radon.core.ContextParam;


@Path("/tunnel")
public class TunnelWeb implements Webapp {

	private ReadSession rsession;
	public TunnelWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}
	
	@POST
	@Path("{fqn : .*}")
	public String editField(@PathParam("fqn") final String fqn, @Context final HttpRequest request){
		rsession.tran(wsession -> {
			WriteNode wnode = wsession.pathBy("/" + fqn) ;
			MultivaluedMap<String, String> params = request.getDecodedFormParameters() ;
			for(String key : params.keySet()){
				if (params.get(key).size() == 1) wnode.property(key, params.getFirst(key)).merge();
				else wnode.property(key, params.get(key).toArray(new String[0])).merge();
			}
		}) ;
		
		return fqn + " edited" ;
	}
	
	
	@GET
	@Path("{fqn : .*}")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public Response viewNode(@PathParam("fqn") String fqn){
		ReadNode node = rsession.pathBy("/" + fqn) ;
		if (! node.exist()) return Response.status(404).build() ;
		
		return Response.ok(node.transformer(Functions.toJson())).build() ;
	}
	

	
	@GET
	@Path("{fqn : .*}.node")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public Response viewAsFormat(@PathParam("fqn") String fqn){
		ReadNode node = rsession.pathBy("/" + fqn) ;
		if (! node.exist()) return Response.status(404).build() ;
		
		return Response.ok(node.transformer(Functions.toJson())).build() ;
	}
	
	

	
	@GET
	@Path("{fqn : .*}.list")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public Response listAsFormat(@PathParam("fqn") String fqn, @DefaultValue("101") @QueryParam("offset") int offset, @DefaultValue("0") @QueryParam("skip") int skip, 
			@DefaultValue("") @QueryParam("ascending") String ascending, @DefaultValue("") @QueryParam("descending") String descending){
		ReadNode node = rsession.pathBy("/" + fqn) ;
		if (! node.exist()) return Response.status(404).build() ;
		
		
		
		
		
		return Response.ok("format").build() ;
	}
	
	
}
