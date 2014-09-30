package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.google.common.base.Function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.webapp.REntry;
import net.ion.radon.core.ContextParam;

@Path("/traces")
public class TraceWeb {

	private ReadSession rsession;
	public TraceWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}
	
	@GET
	@Path("/{userid}")
	public JsonObject recentActivity(@PathParam("userid") String userId){
		Function<Iterator<ReadNode>, JsonObject> fn = new Function<Iterator<ReadNode>, JsonObject>(){
			@Override
			public JsonObject apply(Iterator<ReadNode> iter) {
				JsonObject result = new JsonObject() ;
				while(iter.hasNext()){
					ReadNode node = iter.next() ;
					
					
				}
				return result;
			}
		};
		return rsession.pathBy("/traces/"+ userId).children().descending("time").offset(5).transform(fn) ;
		
	}
}
