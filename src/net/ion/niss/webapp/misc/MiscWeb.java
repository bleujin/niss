package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.schema.JsonPreAnalyzedParser;

import com.google.common.base.Function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

@Path("/misc")
public class MiscWeb implements Webapp{

	
	private ReadSession rsession;
	public MiscWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
	}
	
	@GET
	@Path("/thread")
	public JsonObject listThreadDump() throws IOException{
		return new ThreadDumpInfo().list() ;
	}
	
	
	@GET
	@Path("/properties")
	public JsonObject listProperties(){
		return new PropertyInfo().list() ;
				
	}
	

	@GET
	@Path("/history")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject logHistory(@DefaultValue("") @QueryParam("searchQuery") String query) throws IOException, ParseException{
		
		JsonArray jarray = rsession.ghostBy("/events/loaders").childQuery(query).descending("time").offset(1000).find().transformer(new Function<ChildQueryResponse, JsonArray>(){
			@Override
			public JsonArray apply(ChildQueryResponse res) {
				List<ReadNode> nodes = res.toList() ;
				JsonArray his = new JsonArray() ;
				for(ReadNode node : nodes){
					JsonArray row = new JsonArray() ;
					row.add(new JsonPrimitive(node.fqn().name()))
						.add(new JsonPrimitive(node.property("time").asLong(0)))
						.add(new JsonPrimitive(node.ref("loader").fqn().toString()))
						.add(new JsonPrimitive(node.property("status").asString())) ;
					his.add(row) ;
				}
				return his;
			}
		}) ;
		
		JsonObject result = new JsonObject() ;
		result.add("history", jarray); 
		result.put("schemaName", JsonParser.fromString("[{'title':'Id'},{'title':'Time'},{'title':'LoaderId'},{'title':'Status'}]").getAsJsonArray()) ;
		result.put("info", rsession.ghostBy("/menus/loaders").property("history").asString());
		return result ;
	}
	


	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject userList() throws IOException, ParseException{

		JsonArray jarray = rsession.ghostBy("/users").children().transform(new Function<Iterator<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				while(iter.hasNext()){
					ReadNode node = iter.next() ;
					JsonArray userProp = new JsonArray() ;
					userProp.add(new JsonPrimitive(node.fqn().name())) ;
					userProp.add(new JsonPrimitive(node.property("name").asString())) ;
					result.add(userProp) ;
				}

				return result;
			}
		}) ;
		return new JsonObject().put("info", rsession.ghostBy("/menus/misc").property("user").asString())
				.put("schemaName", JsonParser.fromString("[{'title':'id'},{'title':'name'}]").getAsJsonArray())
				.put("users", jarray) ;
	}
	
	@POST
	@Path("/users/{uid}")
	public String addUser(@PathParam("uid") final String userId, @FormParam("name") final String name, @FormParam("password") final String password){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/users/" + userId)
					.property("name", name)
					.property("password", password)
					.property("registered", System.currentTimeMillis()) ;
				return null ;
			}
		}) ;

		return "registered " + userId ;
	}
	
	
	@POST
	@Path("/users_remove/")
	public String removeUsers(@FormParam("users") final String users){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				String[] targets = StringUtil.split(users, ",") ;
				for (String userId : targets) {
					wsession.pathBy("/users/" + userId).removeSelf() ;
				}
				return null ;
			}
		});
		return "removed " + users ; 
	}
	
	
	
	@DELETE
	@Path("/users/{uid}")
	public String removeUser(@PathParam("uid") final String userId) throws InterruptedException, ExecutionException{
		Boolean removed = rsession.tran(new TransactionJob<Boolean>() {
			@Override
			public Boolean handle(WriteSession wsession) throws Exception {
				return wsession.pathBy("/users/" + userId).removeSelf() ;
			}
		}).get() ;
		
		return removed ? "removed " + userId : "";
	}
	
	
}