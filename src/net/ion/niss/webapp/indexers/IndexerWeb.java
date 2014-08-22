package net.ion.niss.webapp.indexers;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.Source;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.tree.Fqn;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.AnalysisWeb;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.CSVStreamOut;
import net.ion.niss.webapp.common.JsonStreamOut;
import net.ion.niss.webapp.common.SourceStreamOut;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.reader.InfoReader.InfoHandler;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.radon.core.ContextParam;
import net.ion.radon.util.csv.CsvReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

import com.google.common.base.Function;

@Path("/indexers")
public class IndexerWeb implements Webapp {

	private ReadSession rsession;
	private IndexManager imanager;

	public IndexerWeb(@ContextParam("rentry") REntry rentry) throws IOException {
		this.rsession = rentry.login();
		this.imanager = rentry.indexManager() ;
	}

	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray listIndexer() {
		ReadChildren children = rsession.ghostBy("/indexers").children();

		return children.transform(new Function<Iterator<ReadNode>, JsonArray>() {
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray();
				while (iter.hasNext()) {
					ReadNode node = iter.next();
					String name = node.fqn().name();
					result.add(new JsonObject().put("iid", name).put("name", name));
				}
				return result;
			}
		});
	}

	// create indexer
	@POST
	@Path("/{iid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String create(@PathParam("iid") final String iid, @Context HttpRequest req) {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(iid)).property("created", System.currentTimeMillis());
				return null;
			}
		});
		return "created " + iid;
	}
	
	
	// remove colletion
	@DELETE
	@Path("/{iid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String remove(@PathParam("iid") final String iid){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(iid)).removeSelf() ;
				return null;
			}
		}) ;
		
		return "removed " + iid;
	}
	
	

	// --- overview
	@GET
	@Path("/{iid}/status")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewStatus(@PathParam("iid") String iid) throws IOException{
		 return imanager.index(iid).newReader().info(new InfoHandler<JsonObject>() {
				@Override
				public JsonObject view(IndexReader ireader, DirectoryReader dreader) throws IOException {
					JsonObject json = new JsonObject() ;
					
					json.put("Max Doc", dreader.maxDoc()) ;
					json.put("Nums Docs", dreader.numDocs()) ;
					json.put("Deleted Docs",  dreader.numDeletedDocs()) ;
					json.put("Version", dreader.getVersion()) ;
					json.put("Segment Count", dreader.getIndexCommit().getSegmentCount()) ;
					json.put("Current", dreader.isCurrent()) ;
					
					return json;
				}
			});
	}

	
	@GET
	@Path("/{iid}/dirInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewDirInfo(@PathParam("iid") String iid) throws IOException{
		return imanager.index(iid).newReader().info(new InfoHandler<JsonObject>() {
			@Override
			public JsonObject view(IndexReader ireader, DirectoryReader dreader) throws IOException {
				JsonObject json = new JsonObject() ;
				
				json.put("LockFactory", dreader.directory().getLockFactory().getClass().getCanonicalName()) ;
				json.put("Diretory Impl", dreader.directory().getClass().getCanonicalName()) ;
				
				return json;
			}
		});
	}
	
	
	@GET
	@Path("/{iid}/overview")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject overview(@PathParam("iid") final String iid) throws IOException{
		
		return imanager.index(iid).newReader().info(new InfoHandler<JsonObject>() {
			@Override
			public JsonObject view(IndexReader ireader, DirectoryReader dreader) throws IOException {
				JsonObject result = new JsonObject() ;

				result.put("info", rsession.ghostBy("/menus/indexers").property("overview").asString()) ;
				
				JsonObject status = new JsonObject() ;
				status.put("Max Doc", dreader.maxDoc()) ;
				status.put("Nums Docs", dreader.numDocs()) ;
				status.put("Deleted Docs",  dreader.numDeletedDocs()) ;
				status.put("Version", dreader.getVersion()) ;
				status.put("Segment Count", dreader.getIndexCommit().getSegmentCount()) ;
				status.put("Current", dreader.isCurrent()) ;
				result.add("status", status);
				
				JsonObject dirInfo = new JsonObject() ;
				dirInfo.put("LockFactory", dreader.directory().getLockFactory().getClass().getCanonicalName()) ;
				dirInfo.put("Diretory Impl", dreader.directory().getClass().getCanonicalName()) ;
				result.add("dirInfo", dirInfo);

				ReadNode readNode = rsession.pathBy("/indexers/" + iid) ;
				
				JsonArray alist = new JsonArray() ;
				List<Class<? extends Analyzer>> list = AnalysisWeb.analysis() ;
				String selected = readNode.property("indexanalyzer").defaultValue(MyKoreanAnalyzer.class.getCanonicalName()) ;
				for (Class<? extends Analyzer> clz : list) {
					JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", clz.getCanonicalName().equals(selected)) ;
					alist.add(json) ;
				}
				result.put("analyzer", alist);

				result.put("indexanalyzer", readNode.property("indexanalyzer").asString());
				result.put("applystopword", readNode.property("applystopword").asBoolean());

				
				return result;
			}
		});
	}
	
	
	@POST
	@Path("/{iid}/fields")
	@Produces(MediaType.TEXT_PLAIN)
	public String updateField(@PathParam("iid") final String iid, @FormParam("field") final String field, @FormParam("content") final String value) throws Exception{
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(iid)).property(field, value) ;
				return null;
			}
		}) ;
		return value.trim() ;
	}
	
	

	// --- schema
	@GET
	@Path("/{iid}/schema")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject schemaList(@PathParam("iid") String iid){
		
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/indexers").property("schema").asString()) ;
		JsonArray fields = rsession.ghostBy("/indexers/" + iid + "/schema").children().eachNode(new ReadChildrenEach<JsonArray>() {
			@Override
			public JsonArray handle(ReadChildrenIterator iter) {
				JsonArray result = new JsonArray() ;
				for(ReadNode node : iter){
					StringBuilder options = new StringBuilder() ;
					options.append(node.property("store").asBoolean() ? "Store:Yes" : "Store:No") ;   
					options.append(node.property("analyze").asBoolean() ? ", Analyze:Yes" : ", Analyze:No") ;   
					options.append(", Boost:" + StringUtil.defaultIfEmpty(node.property("boost").asString(), "1.0")) ;   

					result.add(new JsonObject().put("fieldid", node.fqn().name()).put("ftype", node.property("ftype").asString()).put("options", options.toString())) ;
				}
				return result;
			}
		}) ;
		
		result.put("fields", fields) ;
		return result ;
	}
	
	
	@POST
	@Path("/{iid}/schema")
	@Produces(MediaType.TEXT_PLAIN)
	public String addSchema(@PathParam("iid") final String iid, @FormParam("fieldid") final String fieldid, @FormParam("ftype") final String ftype, @FormParam("analyze") final boolean analyzer, @FormParam("store") final boolean store, @FormParam("boost") final double boost){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/indexers/" + iid + "/schema/" + fieldid)
					.property("ftype", ftype).property("analyze", analyzer).property("store", store).property("boost", boost) ;
				return null;
			}
		}) ;
		
		return "created schema " + fieldid ;
	}
	

	@DELETE
	@Path("/{iid}/schema/{fieldid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String deleteSchema(@PathParam("iid") final String iid, @PathParam("fieldid") final String fieldid){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/indexers/" + iid + "/schema/" + fieldid).removeSelf() ;
				return null;
			}
		}) ;
		
		return "removed schema " + fieldid ;
	}
	
	
	
	// --- index 
	
	@GET
	@Path("/{iid}/index")
	@Produces(MediaType.TEXT_PLAIN)
	public JsonObject indexView(@PathParam("iid") String iid){
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/indexers").property("index").asString()) ;
		return result ;
	}
	
	
	@POST
	@Path("/{iid}/index.json")
	@Produces(MediaType.TEXT_PLAIN)
	public String indexJson(@PathParam("iid") String iid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request){

		Indexer indexer = imanager.index(iid).newIndexer() ;
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
	@Path("/{iid}/index.jarray")
	@Produces(MediaType.TEXT_PLAIN)
	public String indexJarray(@PathParam("iid") String iid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request){
		Indexer indexer = imanager.index(iid).newIndexer() ;
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
	@Path("/{iid}/index.csv")
	@Produces(MediaType.TEXT_PLAIN)
	public String indexCsv(@PathParam("iid") String iid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request) throws IOException{
		Indexer indexer = imanager.index(iid).newIndexer() ;
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
	@Path("/{iid}/query")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject query(@PathParam("iid") String iid) throws IOException{
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/indexers").property("query").asString()) ;
		return result ;
	}
	
	@GET
	@Path("/{iid}/query.json")
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput jquery(@PathParam("iid") String iid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = imanager.index(iid).newSearcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;

		JsonObject result = sresponse.transformer(Responses.toJson(map, sresponse)) ;
		return new JsonStreamOut(result, indent) ;
	}
	

	@GET
	@Path("/{iid}/query.xml")
	@Produces(MediaType.APPLICATION_XML)
	public StreamingOutput xquery(@PathParam("iid") String iid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
		
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = imanager.index(iid).newSearcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;
		
		Source result = sresponse.transformer(Responses.toXMLSource(map, sresponse));
		return new SourceStreamOut(result, indent) ;
	}
	

	@GET
	@Path("/{iid}/query.csv")
	@Produces(MediaType.TEXT_PLAIN)
	public StreamingOutput cquery(@PathParam("iid") String iid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
		
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0) map.putAll(request.getFormParameters());
		SearchResponse sresponse = imanager.index(iid).newSearcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;
		
		return new CSVStreamOut(sresponse) ;
	}
	

	
	
	
	@GET
	@Path("/{iid}/hello")
	public String hello(@PathParam("iid") String iid){
		return iid ;
	}
	
	
	
	private Fqn fqnBy(String iid) {
		return Fqn.fromString("/indexers/" + IdString.create(iid).idString());
	}

}










