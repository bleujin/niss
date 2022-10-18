package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

import net.bleujin.rcraken.Fqn;
import net.bleujin.rcraken.Property;
import net.bleujin.rcraken.Property.PType;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.WriteNode;
import net.bleujin.rcraken.extend.ChildQueryResponse;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.DateUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.NissServer;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.radon.core.ContextParam;

@Path("/misc")
public class MiscWeb implements Webapp{

	
	private ReadSession rsession;
	private ReadSession dsession;
	public MiscWeb(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
		this.dsession = rentry.login("datas") ;
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
	@Path("/data")
	public String queryContent(@DefaultValue("") @QueryParam("fqn") String fqnPath){
		if (fqnPath.contains(".")) { // property path
			String fqn = StringUtil.substringBeforeLast(fqnPath, ".") ;
			String pid = StringUtil.substringAfterLast(fqnPath, ".") ;
			return dsession.pathBy(fqn).property(pid).asString() ;
		} else {
			return dsession.pathBy(fqnPath).toJson().toString() ; 
		}
	}
	
	@POST
	@Path("/data")
	public String editData(@FormParam("fqn") final String fqnPath, @FormParam("dcontent") final String dcontent){
		String fpath = Fqn.from(fqnPath).name() ;
		if (fpath.contains(".")) { // property path
			String fqn = StringUtil.substringBeforeLast(fqnPath, ".") ;
			String pid = StringUtil.substringAfterLast(fqnPath, ".") ;
			dsession.tran(wsession -> {
				wsession.pathBy(fqn).changeValue(pid, dcontent).merge() ;
			}) ;
			
		} else {
			dsession.tran(wsession -> {
				wsession.readFrom(JsonObject.fromString(dcontent)).merge() ; // ReadNode.toJson() -> jsonObject -> WriteNode.readFrom() 
			}) ;
		}
		
		return "edited " + fqnPath ;
	}
		
	
	
	@GET
	@Path("/shutdown")
	public String shutdown(@Context HttpRequest request,
			@DefaultValue("") @QueryParam("password") String password, 
			@DefaultValue("1000") @QueryParam("time") final int time, @ContextParam("net.ion.niss.NissServer") final NissServer server){
		
		if (! password.equals(server.config().serverConfig().password())) {
			return "not matched password" ;
		}
		
		new Thread(){
			public void run(){
				try {
					Thread.sleep(time);
					server.shutdown() ;
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.start(); 
		
		return "bye after " + time;
	}
	

	@GET
	@Path("/history")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject logHistory(@DefaultValue("") @QueryParam("searchQuery") String query) throws IOException, ParseException{
		
		JsonArray jarray = rsession.pathBy("/events/loaders").childQuery(query).descending("time").offset(1000).find().transformer(new com.google.common.base.Function<ChildQueryResponse, JsonArray>(){
			@Override
			public JsonArray apply(ChildQueryResponse res) {
				List<ReadNode> nodes = res.toList() ;
				JsonArray his = new JsonArray() ;
				for(ReadNode node : nodes){
					JsonArray row = new JsonArray() ;
					row.add(new JsonPrimitive(node.fqn().name()))
						.add(new JsonPrimitive(DateUtil.timeMilliesToDay(node.property("time").asLong())))
						.add(new JsonPrimitive(node.property("loader").asString()))
						.add(new JsonPrimitive(node.property("status").asString())) ;
					his.add(row) ;
				}
				return his;
			}
		}) ;
		
		JsonObject result = new JsonObject() ;
		result.add("history", jarray); 
		result.put("schemaName", JsonParser.fromString("[{'title':'Id'},{'title':'Time'},{'title':'LoaderId'},{'title':'Status'}]").getAsJsonArray()) ;
//		result.put("info", rsession.pathBy("/menus/loaders").property("history").asString());
		return result ;
	}
	


	@GET
	@Path("/users")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject userList() throws IOException, ParseException{

		JsonArray jarray = rsession.pathBy("/users").children().stream().transform(new Function<Iterable<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterable<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				for(ReadNode node : iter){
					JsonArray userProp = new JsonArray() ;
					userProp.add(new JsonPrimitive(node.fqn().name())) ;
					userProp.add(new JsonPrimitive(node.property("name").asString())) ;
					result.add(userProp) ;
				}

				return result;
			}
		}) ;
		return new JsonObject()
//				.put("info", rsession.pathBy("/menus/misc").property("user").asString())
				.put("schemaName", JsonParser.fromString("[{'title':'Id'},{'title':'Name'}]").getAsJsonArray())
				.put("users", jarray) ;
	}
	
	@POST
	@Path("/users/{uid}")
	public String addUser(@PathParam("uid") final String userId, @FormParam("name") final String name, @FormParam("password") final String password){
		rsession.tran(wsession -> {
			wsession.pathBy("/users/" + userId)
				.property("name", name)
				.property("password", password)
				.property("registered", System.currentTimeMillis()).merge();
		}) ;

		return "registered " + userId ;
	}
	
	@POST
	@Path("/profile/{uid}")
	public String editUser(@PathParam("uid") final String userId, @Context HttpRequest request){
		final MultivaluedMap<String, String> formParam = request.getDecodedFormParameters() ;
		
		rsession.tran(wsession -> {
			WriteNode found = wsession.pathBy("/users/" + userId) ;
			for (String key : formParam.keySet()) {
				found.property(key, formParam.getFirst(key)).merge();
			}
		}) ;
		
		return "edited " + userId ;
	}
	
	
	
	@POST
	@Path("/users_remove")
	public String removeUsers(@FormParam("users") final String users){
		rsession.tran( wsession -> {
			String[] targets = StringUtil.split(users, ",") ;
			for (String userId : targets) {
				wsession.pathBy("/users/" + userId).removeSelf() ;
			}
		});
		return "removed " + users ; 
	}
	
	
	
	@DELETE
	@Path("/users/{uid}")
	public String removeUser(@PathParam("uid") final String userId) throws InterruptedException, ExecutionException{
		Boolean removed = rsession.tran(wsession -> {
			wsession.pathBy("/users/" + userId).removeSelf() ;
			return Boolean.TRUE ;
		}).get() ;
		
		return removed ? "removed " + userId : "";
	}
	
	
}
