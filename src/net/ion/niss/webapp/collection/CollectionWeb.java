package net.ion.niss.webapp.collection;

import java.io.IOException;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.apps.collection.CollectionApp;
import net.ion.niss.apps.collection.IndexCollection;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

@Path("/collections")
public class CollectionWeb implements Webapp{

	private CollectionApp app ;
	public CollectionWeb(@ContextParam("CollectionApp") CollectionApp app){
		this.app = app ;
	}
	
	// --- overview
	@GET
	@Path("/{cid}/status")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewStatus(@PathParam("cid") String cid) throws IOException{
		return indexCollection(cid).status() ;
		
	}

	
	@GET
	@Path("/{cid}/dirInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewDirInfo(@PathParam("cid") String cid) throws IOException{
		return indexCollection(cid).dirInfo() ;
		
	}
	
	
	@GET
	@Path("/{cid}/overview")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject overview(@PathParam("cid") String cid) throws IOException{
		JsonObject result = new JsonObject() ;
		IndexCollection found = indexCollection(cid);
		result.put("info", found.propAsString("overview")) ;
		result.add("status", found.status());
		result.add("dirInfo", found.dirInfo());
		
		return result ;
		
	}
	
	
	@POST
	@Path("/{cid}/fields/info")
	@Produces(MediaType.TEXT_PLAIN)
	public String explain(@PathParam("cid") String cid, @FormParam("field") String field, @FormParam("content") String content) throws Exception{
		IndexCollection ic = indexCollection(cid);
		ic.updateExplain(field, content.trim()) ;
		return ic.propAsString(field) ;
	}

	
	
	
	
	
	// --- index analysis
	@GET
	@Path("/{cid}/analysis")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject analysis(@PathParam("cid") String cid) throws IOException{
		JsonObject result = new JsonObject() ;
		IndexCollection found = indexCollection(cid);
		result.put("info", found.propAsString("analysis")) ;
		result.add("analyzer", found.indexAnalyer());
		
		return result ;
	}
	
	@POST
	@Path("/{cid}/analysis")
	public JsonObject tokenAnalyzer(@FormParam("content") String content, @FormParam("analyzer") String clzName, @FormParam("stopword") boolean stopword){
		Debug.line(content, clzName, stopword);
		return new JsonObject() ;
	}
	
	private IndexCollection indexCollection(String cid) {
		return app.find(cid);
	}

}
