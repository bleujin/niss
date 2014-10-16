package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.util.Iterator;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.webapp.REntry;
import net.ion.radon.core.ContextParam;

import com.google.common.base.Function;

@Path("/traces")
public class TraceWeb {

	private ReadSession rsession;
	public TraceWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}
	
	@GET
	@Path("/{userid}")
	public JsonObject recentActivity(@PathParam("userid") String userId, @DefaultValue("5") @QueryParam("offset") int offset){
		Function<Iterator<ReadNode>, JsonArray> fn = new Function<Iterator<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				while(iter.hasNext()){
					ReadNode node = iter.next() ;
					result.add(new JsonObject()
						.put("path", node.fqn().toString())
						.put("uri", node.property("uri").asString())
						.put("time", node.property("time").asLong(0)) 
						.put("address", node.property("address").asString()) 
						.put("method", node.property("method").asString()) 
					) ;
				}
				return result;
			}
		};
		JsonArray jsonTrace = rsession.ghostBy("/traces/"+ userId).children().descending("time").offset(offset).transform(fn) ;
		
		return new JsonObject().put("traces", jsonTrace);
	}
}
