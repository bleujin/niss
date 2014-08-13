package net.ion.niss.webapp.section;

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
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.tree.Fqn;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.apps.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

@Path("/sections")
public class SectionWeb implements Webapp{

	
	private ReadSession rsession;
	public SectionWeb(@ContextParam("rentry") REntry rentry) throws IOException {
		 this.rsession = rentry.login() ;
	}
	
	
	
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray listSection(){
		ReadChildren children = rsession.pathBy("/sections").children() ;
		
		return children.transform(new Function<Iterator<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				while(iter.hasNext()) {
					ReadNode node = iter.next() ;
					result.add(new JsonObject().put("sid", node.fqn().name())) ;
				}
				return result;
			}
		}) ;
	}
	
	
	// create section
	@POST
	@Path("")
	@Produces(MediaType.TEXT_PLAIN)
	public String create(@FormParam("sid") final String sid){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(sid)).property("created", System.currentTimeMillis()) ;
				return null;
			}
		}) ;
		
		return "created " + sid ;
	}

	
	
	// define section
	@POST
	@Path("/{sid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String defineSection(@PathParam("sid") final String sid, @FormParam("collection") final String[] collection
				, @FormParam("filter") final String filter, @DefaultValue("false") @FormParam("applyfilter") final boolean applyFilter
				, @FormParam("sort") final String sort, @DefaultValue("false") @FormParam("applysort") final boolean applySort
				, @FormParam("handler") final String handler, @DefaultValue("false") @FormParam("applyhandler") final boolean applyHandler) {
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode wnode = wsession.pathBy(fqnBy(sid)).property("collection", collection)
					.property("filter", filter).property("applyfilter", applyFilter)
					.property("sort", sort).property("applysort", applySort)
					.property("handler", handler).property("applyhandler", applyHandler) ;

				return null;
			}
		}) ;
		
		return "modified " + sid ;
	}
	
	@GET
	@Path("/{sid}")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewSection(@PathParam("sid") final String sid){
		return rsession.pathBy(fqnBy(sid)).transformer(new Function<ReadNode, JsonObject>(){
			@Override
			public JsonObject apply(ReadNode node) {
				return new JsonObject().put("collection", node.property("collection").asSet().toArray(new String[0]))
						.put("filter", node.property("filter").asString()).put("applyfilter", node.property("applyfilter").asBoolean())
						.put("sort", node.property("sort").asString()).put("applysort", node.property("applysort").asBoolean())
						.put("handler", node.property("handler").asString()).put("applyhandler", node.property("applyhandler").asBoolean())
							;
			}
		}) ;
	}
	
	
	private Fqn fqnBy(String sid) {
		return Fqn.fromString("/sections/" + IdString.create(sid).idString()) ;
	}
}
