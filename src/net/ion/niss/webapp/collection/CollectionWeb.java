package net.ion.niss.webapp.collection;

import java.io.IOException;
import java.util.Iterator;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.tree.Fqn;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.apps.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

@Path("/collections")
public class CollectionWeb implements Webapp{
	
	private ReadSession rsession;
	public CollectionWeb(@ContextParam("rentry") REntry rentry) throws IOException {
		 this.rsession = rentry.login() ;
	}

	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray listCollection(){
		ReadChildren children = rsession.pathBy("/collections").children() ;
		
		return children.transform(new Function<Iterator<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				while(iter.hasNext()) {
					ReadNode node = iter.next() ;
					result.add(new JsonObject().put("cid", node.fqn().name())) ;
				}
				return result;
			}
		}) ;
	}

	// create collection
	@POST
	@Path("")
	@Produces(MediaType.TEXT_PLAIN)
	public String create(@FormParam("cid") final String cid){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(cid)).property("created", System.currentTimeMillis()) ;
				return null;
			}
		}) ;
		
		return "created " + cid ;
	}


	



	
	
	
	
	
	
	


	private Fqn fqnBy(String cid) {
		return Fqn.fromString("/collections/" + IdString.create(cid).idString()) ;
	}
	
}
