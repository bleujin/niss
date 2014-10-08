package net.ion.niss.webapp.loaders;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import javax.script.ScriptException;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.ObjectId;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

import org.apache.lucene.queryparser.classic.ParseException;
import org.infinispan.affinity.RndKeyGenerator;
import org.jboss.resteasy.spi.HttpResponse;

import com.google.common.base.Function;

@Path("/loaders")
public class LoaderWeb implements Webapp {

	private ReadSession rsession;
	private JScriptEngine jengine;
	private EventSourceEntry esentry;
	private REntry rentry;

	public LoaderWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine, @ContextParam("esentry") EventSourceEntry esentry) throws IOException {
		this.rentry = rentry ;
		this.rsession = rentry.login();
		this.jengine = jengine ;
		this.esentry = esentry ;
	}

	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject listScript() {
		JsonArray scripts = rsession.ghostBy("/loaders").children().transform(new Function<Iterator<ReadNode>, JsonArray>() {
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray();
				while (iter.hasNext()) {
					ReadNode node = iter.next();
					result.add(new JsonObject().put("lid", node.fqn().name()).put("name", node.property("name").asString()));
				}
				return result;
			}

		});
		
		JsonObject result = new JsonObject() ;
		result.add("loaders", scripts);
		result.put("info", rsession.ghostBy("/menus/loaders").property("overview").asString()) ;
		
		return result;

	}


	@POST
	@Path("/{lid}")
	public String newLoader(@PathParam("lid") final String lid, @FormParam("name") final String name) {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/loaders/" + lid).property("name", name).property("registered", System.currentTimeMillis());;
				return null;
			}
		});
		return "created " + lid;
	}
	
	@DELETE
	@Path("/{lid}")
	public String removeLoader(@PathParam("lid") final String lid) {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/loaders").removeChild(lid);
				return null;
			}
		});
		return "deleted " + lid;
	}

	
	
	@POST
	@Path("/{lid}/define")
	@Produces(MediaType.TEXT_PLAIN)
	public String createScript(@PathParam("lid") final String lid, @FormParam("name") final String name, @FormParam("content") final String content) {
		rsession.tran(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/loaders").child(lid).property("content", content).property("registered", System.currentTimeMillis());
				return lid;
			}
		});
		return "defined loader : " + lid;
	}



	@GET
	@Path("/{lid}/overview")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject overview(@PathParam("lid") final String lid) {
		
		JsonArray jarray = rsession.ghostBy("/events/loaders").children().eq("lid", lid).descending("time").offset(101).transform(new Function<Iterator<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray his = new JsonArray() ;
				while(iter.hasNext()){
					ReadNode node = iter.next() ;
					JsonArray row = new JsonArray() ;
					row.add(new JsonPrimitive(node.fqn().name()))
						.add(new JsonPrimitive(node.property("time").asLong(0)))
						.add(new JsonPrimitive(node.property("status").asString())) ;
					his.add(row) ;
				}
				return his;
			}
		}) ;
		
		JsonObject result = new JsonObject() ;
		result.add("history", jarray); 
		result.put("schemaName", JsonParser.fromString("[{'title':'evetId'},{'title':'Time'},{'title':'Status'}]").getAsJsonArray()) ;		
		result.put("info", rsession.ghostBy("/menus/loaders").property("overview").asString()) ;
		
		return result ;
	}


	
	@GET
	@Path("/{lid}/define")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewScript(@PathParam("lid") final String lid) {
		ReadNode found = rsession.pathBy("/loaders/" + lid) ;
		
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/loaders").property("define").asString()) ;
		result.put("lid", found.fqn().name()) ;
		result.put("name", found.property("name").asString()) ;
		result.put("content", found.property("content").asString()) ;
		return result ;
	}


	
	@POST
	@Path("/{lid}/run/{eventId}")
	public String run(@PathParam("lid") final String lid, @PathParam("eventId") final String eventId, @FormParam("content") final String content, @Context HttpResponse response) throws IOException, ScriptException{
		InstantJavaScript script = jengine.createScript(IdString.create(lid), "run at " + System.currentTimeMillis(), new StringReader(content)) ;
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/events/loaders/" +eventId)
					.property("lid", lid)
					.property("script", content)
					.property("status", "started").property("time", System.currentTimeMillis()) ;
				return null;
			}
		}) ;
		
		
		final Writer writer = esentry.newWriter(rentry, lid, eventId) ;
		
		Future<Object> future = script.runAsync(writer, new ExceptionHandler() {
			@Override
			public Object handle(final Exception ex) {
				try {
					rsession.tran(new TransactionJob<Void>() {
						@Override
						public Void handle(WriteSession wsession) throws Exception {
							wsession.pathBy("/events/loaders/" +eventId)
								.property("status", "fail")
								.property("exception", ex.getMessage()) ;
							return null;
						}
					}) ;
					
					writer.write(ex.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
		});
		
		return eventId ;

	}

	
	
}
