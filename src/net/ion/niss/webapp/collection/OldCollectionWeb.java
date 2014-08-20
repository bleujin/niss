package net.ion.niss.webapp.collection;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.Source;

import net.ion.craken.node.ReadNode;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.niss.apps.IdString;
import net.ion.niss.apps.old.IndexCollection;
import net.ion.niss.apps.old.IndexManager;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.CSVStreamOut;
import net.ion.niss.webapp.common.JsonStreamOut;
import net.ion.niss.webapp.common.SourceStreamOut;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.radon.core.ContextParam;
import net.ion.radon.util.csv.CsvReader;

import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

@Path("/collections")
public class OldCollectionWeb implements Webapp{

	private IndexManager app ;
	public OldCollectionWeb(@ContextParam("IndexManager") IndexManager app){
		this.app = app ;
	}
	
	
	// outer
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray list(){
		Map<IdString, IndexCollection> map = app.cols() ;
		
		JsonArray result = new JsonArray() ;
		for (Entry<IdString, IndexCollection> entry : map.entrySet()) {
			result.add(new JsonObject().put("cid", entry.getKey().idString()).put("name", entry.getKey().idString())) ;
		}
		
		return result ;
	}
	
	@POST
	@Path("/{cid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String newIndexCollection(@PathParam("cid") String cid) throws Exception{
		IndexCollection created = app.newCollection(cid);
		
		return cid + " created" ;
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
		
		ReadNode readNode = found.infoNode() ;
		result.put("analyzer", found.analyzerList());
		result.put("indexanalyzer", readNode.property("indexanalyzer").asString());
		result.put("applystopword", readNode.property("applystopword").asBoolean());
		
		return result ;
		
	}
	
	
	// prefix header
	@POST
	@Path("/{cid}/info")
	@Produces(MediaType.TEXT_PLAIN)
	public String editInfo(@PathParam("cid") String cid, @FormParam("field") String field, @FormParam("content") String content) throws Exception{
		IndexCollection ic = indexCollection(cid);
		ic.updateExplain(field, content.trim()) ;
		return content.trim() ;
	}

	
	@POST
	@Path("/{cid}/fields")
	@Produces(MediaType.TEXT_PLAIN)
	public String updateField(@PathParam("cid") String cid, @FormParam("field") String field, @FormParam("content") String content) throws Exception{
		IndexCollection ic = indexCollection(cid);
		ic.updateExplain(field, content.trim()) ;
		return content.trim() ;
	}
	
	
	
	
	
	// --- index analysis
	@GET
	@Path("/{cid}/analysis")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject analysis(@PathParam("cid") String cid) throws IOException{
		JsonObject result = new JsonObject() ;
		IndexCollection found = indexCollection(cid);
		result.put("info", found.propAsString("analysis")) ;
		result.add("analyzer", found.analyzerList());
		
		return result ;
	}
	
	@POST
	@Path("/{cid}/analysis")
	public JsonArray tokenAnalyzer(@PathParam("cid") String cid, @FormParam("content") String content, @FormParam("analyzer") String clzName, @FormParam("stopword") boolean stopword) throws Exception{
		JsonArray terms = indexCollection(cid).termAnalyzer(content, clzName, stopword) ;
		return terms ;
	}
	
	private IndexCollection indexCollection(String cid) {
		return app.find(cid);
	}

	
	
	// --- file
	@GET
	@Path("/{cid}/files")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject files(@PathParam("cid") String cid){
		Collection<File> files = app.listFiles(IdString.create(cid)) ;
		
		JsonArray array = new JsonArray() ;
		for (File file : files) {
			array.add(new JsonObject().put("name", file.getName()).put("path", "/" + cid + "/files/" + file.getName()));
		}
		JsonObject result = new JsonObject() ;
		result.put("info", indexCollection(cid).propAsString("files")) ;
		
		return result.put("files", array) ;
	}
	
	
	@GET
	@Path("/{cid}/files/{fileName}")
	@Produces(MediaType.TEXT_PLAIN)
	public File viewFile(@PathParam("cid") String cid, @PathParam("fileName") String fileName){
		File file = app.viewFile(IdString.create(cid), fileName);
		if (! file.exists()) throw new WebApplicationException(404) ;
		return file ;
	}
	
	@POST
	@Path("/{cid}/files/{fileName}")
	@Produces(MediaType.TEXT_PLAIN)
	public String editFile(@PathParam("cid") String cid, @PathParam("fileName") String fileName, @FormParam("filecontent") String content) throws IOException{
		File file = app.viewFile(IdString.create(cid), fileName);
		if (! file.exists()) throw new WebApplicationException(404) ;
		
		FileUtil.write(file, content);
		return "writed" ;
	}
	
	
	// --- index 
	
	@POST
	@Path("/{cid}/index.json")
	@Produces(MediaType.TEXT_PLAIN)
	public String indexJson(@PathParam("cid") String cid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request){

		Indexer indexer = indexCollection(cid).indexer() ;
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				JsonObject json = JsonObject.fromString(documents) ;
				WriteDocument wdoc = isession.newDocument(json.asString("id")) ;
				wdoc.add(json) ;
				
				if (overwrite) isession.updateDocument(wdoc) ;
				else isession.insertDocument(wdoc) ;
				return null;
			}
		}) ;
		
		return "1 indexed" ;
	}
	
	@POST
	@Path("/{cid}/index.jarray")
	@Produces(MediaType.TEXT_PLAIN)
	public String indexJarray(@PathParam("cid") String cid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request){
		Indexer indexer = indexCollection(cid).indexer() ;
		final JsonArray jarray = JsonParser.fromString(documents).getAsJsonArray() ;
		
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				for (int i=0 ; i <jarray.size() ; i++) {
					JsonObject json = jarray.get(i).getAsJsonObject() ;
					WriteDocument wdoc = isession.newDocument(json.asString("id")) ;
					wdoc.add(json) ;
					
					if (overwrite) isession.updateDocument(wdoc) ;
					else isession.insertDocument(wdoc) ;
				}
				
				return null;
			}
		}) ;
		
		return jarray.size() + " indexed" ;
	}
	
	@POST
	@Path("/{cid}/index.csv")
	@Produces(MediaType.TEXT_PLAIN)
	public String indexCsv(@PathParam("cid") String cid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request) throws IOException{
		Indexer indexer = indexCollection(cid).indexer() ;
		final CsvReader creader = new CsvReader(new StringReader(documents)) ;
		final String[] headers = creader.readLine() ;
		
		int sum = indexer.index(new IndexJob<Integer>() {
			@Override
			public Integer handle(IndexSession isession) throws Exception {
				String[] fields ;
				int count = 0 ;
				while((fields = creader.readLine()) != null){
					Map<String, String> map = MapUtil.newMap() ;
					for (int i = 0 ; i < headers.length ; i++) {
						map.put(headers[i], i < fields.length ? fields[i] : "") ;
					}
					
					count++ ;
					WriteDocument wdoc = isession.newDocument(map.get("id")).add(map) ;
					
					if (overwrite) isession.updateDocument(wdoc) ;
					else isession.insertDocument(wdoc) ;
				}
				return count;
			}
		}) ;
		
		return sum + " indexed" ;
	}
	
	
	
	// --- query
	
	@GET
	@Path("/{cid}/query")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject query(@PathParam("cid") String cid) throws IOException{
		JsonObject result = new JsonObject() ;
		IndexCollection found = indexCollection(cid);
		result.put("info", found.propAsString("query")) ;
		
		return result ;
	}
	
	@GET
	@Path("/{cid}/query.json")
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput jquery(@PathParam("cid") String cid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = indexCollection(cid).searcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;

		JsonObject result = sresponse.transformer(Responses.toJson(map, sresponse)) ;
		return new JsonStreamOut(result, indent) ;
	}
	

	@GET
	@Path("/{cid}/query.xml")
	@Produces(MediaType.APPLICATION_XML)
	public StreamingOutput xquery(@PathParam("cid") String cid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
		
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = indexCollection(cid).searcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;
		
		Source result = sresponse.transformer(Responses.toXMLSource(map, sresponse));
		return new SourceStreamOut(result, indent) ;
	}
	

	@GET
	@Path("/{cid}/query.csv")
	@Produces(MediaType.TEXT_PLAIN)
	public StreamingOutput cquery(@PathParam("cid") String cid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
		
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = indexCollection(cid).searcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;
		
		return new CSVStreamOut(sresponse) ;
	}
	

	
	
	
	@GET
	@Path("/{cid}/hello")
	public String hello(@PathParam("cid") String cid){
		return cid ;
	}
}
