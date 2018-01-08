package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.util.function.Function;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

@Path("/traces")
public class TraceWeb implements Webapp {

	private ReadSession rsession;
	public TraceWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}
	
	@GET
	@Path("/{userid}")
	public JsonObject recentActivity(@PathParam("userid") String userId, @DefaultValue("5") @QueryParam("offset") int offset){
		Function<Iterable<ReadNode>, JsonArray> fn = new Function<Iterable<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterable<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				for(ReadNode node : iter){
					result.add(new JsonObject()
						.put("path", node.fqn().toString())
						.put("uri", node.property("uri").asString())
						.put("time", node.property("time").asLong()) 
						.put("address", node.property("address").asString()) 
						.put("method", node.property("method").asString()) 
					) ;
				}
				return result;
			}
		};
		JsonArray jsonTrace = rsession.pathBy("/traces/"+ userId).children().stream().descending("time").limit(offset).transform(fn) ;
		
		return new JsonObject().put("traces", jsonTrace);
	}
	
	@Path("/{userid}")
	@DELETE
	public String removeActivity(@PathParam("userid") final String userId){
		rsession.tran(wsession -> {
			wsession.pathBy("/traces/" + userId).removeChild();
		})  ;
		
		return "removed " + userId;
	}
}
