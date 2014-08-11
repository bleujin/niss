package net.ion.niss.webapp.collection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.ecs.xml.XML;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

import net.ion.craken.node.ReadNode;
import net.ion.framework.parse.gson.Gson;
import net.ion.framework.parse.gson.GsonBuilder;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.apps.IdString;
import net.ion.niss.apps.collection.IndexCollectionApp;
import net.ion.niss.apps.collection.IndexCollection;
import net.ion.niss.webapp.Webapp;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.radon.core.ContextParam;
import net.ion.radon.util.csv.CsvReader;
import net.ion.radon.util.csv.CsvWriter;

@Path("/collections")
public class CollectionWeb implements Webapp{

	private IndexCollectionApp app ;
	public CollectionWeb(@ContextParam("IndexCollectionApp") IndexCollectionApp app){
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

class CSVStreamOut implements StreamingOutput {

	private SearchResponse sresponse;
	public CSVStreamOut(SearchResponse sresponse) {
		this.sresponse = sresponse ;
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		CsvWriter cwriter = new CsvWriter(new BufferedWriter(new OutputStreamWriter(output))) ;

		Set<String> nameSet = SetUtil.newOrdereddSet() ;
		for(ReadDocument doc : sresponse.getDocument()) {
			nameSet.addAll(ListUtil.toList(doc.getFieldNames())) ;
		}
		
		cwriter.writeLine(nameSet.toArray(new String[0]));
		for(ReadDocument doc : sresponse.getDocument()) {
			for (String fname : nameSet) {
				String value = doc.get(fname);
				cwriter.writeField(value == null ? "" : value);
			}
			cwriter.endBlock(); 
		}
		cwriter.flush(); 
	}
	
}


class SourceStreamOut implements StreamingOutput {

	private Source source;
	private boolean indent;

	public SourceStreamOut(Source source, boolean indent) {
		this.source = source ;
		this.indent = indent ;
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		try {
			StreamResult xmlOutput = new StreamResult(output);
			Transformer transformer = SAXTransformerFactory.newInstance().newTransformer();
			if (indent) {
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2") ;
			}

//			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.transform(source, xmlOutput);
		} catch (TransformerException e) {
			throw new IOException(e) ;
		} catch (TransformerFactoryConfigurationError e) {
			throw new IOException(e) ;
		}
	}
	
}


class JsonStreamOut implements StreamingOutput {

	private JsonObject json;
	private boolean indent ;
	public JsonStreamOut(JsonObject json, boolean indent) {
		this.json = json ;
		this.indent = indent ;
	}
	
	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(output, "UTF-8")) ;
		if(indent) jwriter.setIndent("  ");
		
		jwriter.beginObject() ;
		for (Entry<String, JsonElement> entry : json.entrySet()) {
			writeJsonElement(jwriter, json, entry.getKey(), entry.getValue()) ; 
		}
		jwriter.endObject() ;
		jwriter.flush(); 
	}
	
	
	private void writeJsonElement(JsonWriter jwriter, JsonElement parent, String name, JsonElement json) throws IOException {
		if (json.isJsonPrimitive()){
			if (parent.isJsonObject()) jwriter.name(name) ;
			final JsonPrimitive preEle = json.getAsJsonPrimitive();
			if (preEle.isBoolean()){
				jwriter.value(preEle.getAsBoolean()) ;
			} else if (preEle.isNumber()) {
				jwriter.value(preEle.getAsNumber()) ;
			} else {
				jwriter.value(preEle.getAsString()) ;
			} 
		} else if (json.isJsonObject()){
			if (parent.isJsonObject()) jwriter.name(name) ;
			jwriter.beginObject() ;
			for(Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()){
				writeJsonElement(jwriter, json, entry.getKey(), entry.getValue()) ;
			}
			jwriter.endObject() ;
		} else if (json.isJsonArray()){
			if (parent.isJsonObject()) jwriter.name(name) ;
			jwriter.beginArray() ;
			for(JsonElement ele : json.getAsJsonArray()){
				writeJsonElement(jwriter, json, name, ele) ;
			} 
			jwriter.endArray() ;
		} else if (json.isJsonNull()){
			; // ignore
		}
	}
}
