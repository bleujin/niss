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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.Source;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.tree.Fqn;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.apps.IdString;
import net.ion.niss.apps.collection.SearchManager;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.collection.Responses;
import net.ion.niss.webapp.common.CSVStreamOut;
import net.ion.niss.webapp.common.JsonStreamOut;
import net.ion.niss.webapp.common.SourceStreamOut;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.radon.core.ContextParam;

import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

import com.google.common.base.Function;

@Path("/sections")
public class SectionWeb implements Webapp{

	
	private ReadSession rsession;
	private SearchManager smanager;
	
	public SectionWeb(@ContextParam("rentry") REntry rentry) throws IOException {
		 this.rsession = rentry.login() ;
		 this.smanager = rentry.searchManager() ;
	}
	
	
	
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray listSection(){
		ReadChildren children = rsession.ghostBy("/sections").children() ;
		
		return children.transform(new Function<Iterator<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				while(iter.hasNext()) {
					ReadNode node = iter.next() ;
					result.add(new JsonObject().put("sid", node.fqn().name()).put("name", node.fqn().name())) ;
				}
				return result;
			}
		}) ;
	}
	
	
	// create section

	@POST
	@Path("/{sid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String create(@PathParam("sid") final String sid){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(sid)).property("created", System.currentTimeMillis()) ;
				return null;
			}
		}) ;
		
		return "created " + sid ;
	}

	
	
	
	@GET
	@Path("/{sid}/define")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewSection(@PathParam("sid") final String sid){
		
		final String[] colNames = rsession.ghostBy("/collections").childrenNames().toArray(new String[0]) ;
		
		return rsession.pathBy(fqnBy(sid)).transformer(new Function<ReadNode, JsonObject>(){
			@Override
			public JsonObject apply(ReadNode node) {
				return new JsonObject()
						.put("info", rsession.ghostBy("/menus/sections").property("define").asString())
						.put("collections", colNames)
						.put("collection", node.property("collection").asSet().toArray(new String[0]))
						.put("filter", node.property("filter").asString()).put("applyfilter", node.property("applyfilter").asBoolean())
						.put("sort", node.property("sort").asString()).put("applysort", node.property("applysort").asBoolean())
						.put("handler", node.property("handler").asString()).put("applyhandler", node.property("applyhandler").asBoolean())
							;
			}
		}) ;
	}
	
	// define section
	@POST
	@Path("/{sid}/define")
	@Produces(MediaType.TEXT_PLAIN)
	public String defineSection(@PathParam("sid") final String sid, @FormParam("target_collection") final String collections
				, @Context HttpRequest request
				, @FormParam("filter") final String filter, @DefaultValue("false") @FormParam("applyfilter") final boolean applyFilter
				, @FormParam("sort") final String sort, @DefaultValue("false") @FormParam("applysort") final boolean applySort
				, @FormParam("handler") final String handler, @DefaultValue("false") @FormParam("applyhandler") final boolean applyHandler) {
		
		final String[] collection = StringUtil.split(collections, ",") ;
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

	
	
	// --- query
	@GET
	@Path("/{sid}/query")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject query() throws IOException{
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/sections").property("query").asString()) ;
		return result ;
	}
	
	@GET
	@Path("/{sid}/query.json")
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput jquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = smanager.searcher(sid).createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;

		JsonObject result = sresponse.transformer(Responses.toJson(map, sresponse)) ;
		return new JsonStreamOut(result, indent) ;
	}
	

	@GET
	@Path("/{sid}/query.xml")
	@Produces(MediaType.APPLICATION_XML)
	public StreamingOutput xquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
		
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = smanager.searcher(sid).createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;
		
		Source result = sresponse.transformer(Responses.toXMLSource(map, sresponse));
		return new SourceStreamOut(result, indent) ;
	}
	

	@GET
	@Path("/{sid}/query.csv")
	@Produces(MediaType.TEXT_PLAIN)
	public StreamingOutput cquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
		
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = smanager.searcher(sid).createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;
		
		return new CSVStreamOut(sresponse) ;
	}
	
	@GET
	@Path("/{sid}/query.template")
	@Produces(MediaType.TEXT_PLAIN)
	public String tquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
	
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = smanager.searcher(sid).createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;

		String template = rsession.pathBy(fqnBy(sid)).property("template").asString() ;
		
		Engine engine = rsession.workspace().parseEngine();
		return engine.transform(template, MapUtil.<String, Object>chainMap().put("response", sresponse).put("params", map).toMap()) ;
	}

	
	@GET
	@Path("/{sid}/query.velocity")
	@Produces(MediaType.TEXT_PLAIN)
	public String velocity(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
	
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = smanager.searcher(sid).createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;

		String template = rsession.pathBy(fqnBy(sid)).property("template").asString() ;
		
		Engine engine = rsession.workspace().parseEngine();
		return engine.transform(template, MapUtil.<String, Object>chainMap().put("response", sresponse).put("params", map).toMap()) ;
	}

	
	
	
	
	
	// -- template
	@GET
	@Path("/{sid}/template")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewTemplate(@PathParam("sid") final String sid){
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/sections").property("template").asString()) ;
		result.put("template", rsession.pathBy(fqnBy(sid)).property("template").asString()) ;
		return result ;
	}
	
	@GET
	@Path("/{sid}/template.default")
	@Produces(MediaType.TEXT_PLAIN)
	public String defaultTemplate(@PathParam("sid") final String sid) throws IOException{
		return IOUtil.toStringWithClose(getClass().getResourceAsStream("default.template")) ;
	}
	
	
	@POST
	@Path("/{sid}/template")
	public String editTemplate(@PathParam("sid") final String sid, @FormParam("template") final String template){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(sid)).property("template", template) ;
				return null;
			}
		}) ;
		return "modified template" ;
	}
	
	
	private Fqn fqnBy(String sid) {
		return Fqn.fromString("/sections/" + IdString.create(sid).idString()) ;
	}
}
