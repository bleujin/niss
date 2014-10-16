package net.ion.niss.webapp.indexers;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.CSVStreamOut;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.Schema;
import net.ion.niss.webapp.common.JsonStreamOut;
import net.ion.niss.webapp.common.SourceStreamOut;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.nsearcher.common.FieldIndexingStrategy;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.reader.InfoReader.InfoHandler;
import net.ion.nsearcher.search.ISearchable;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.TransformerKey;
import net.ion.radon.core.ContextParam;
import net.ion.radon.util.csv.CsvReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;
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
	public JsonObject viewDirInfo(@PathParam("iid") final String iid) throws IOException{
		return imanager.index(iid).newReader().info(new InfoHandler<JsonObject>() {
			@Override
			public JsonObject view(IndexReader ireader, DirectoryReader dreader) throws IOException {
				JsonObject json = new JsonObject() ;
				
				json.put("LockFactory", dreader.directory().getLockFactory().getClass().getCanonicalName()) ;
				json.put("Diretory Impl", dreader.directory().getClass().getCanonicalName()) ;
				if (dreader.directory() instanceof FSDirectory){
					json.put("FileStore Path", ((FSDirectory)dreader.directory()).getDirectory()) ;
				}
				
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
				if (dreader.directory() instanceof FSDirectory){
					dirInfo.put("FileStore Path", ((FSDirectory)dreader.directory()).getDirectory().getCanonicalPath()) ;
				}
				
				result.add("dirInfo", dirInfo);

				ReadNode readNode = rsession.pathBy("/indexers/" + iid) ;
				
				JsonArray alist = new JsonArray() ;
				List<Class<? extends Analyzer>> list = AnalysisWeb.analysis() ;
				String selected = readNode.property(Def.Indexer.IndexAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()) ;
				for (Class<? extends Analyzer> clz : list) {
					JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", clz.getCanonicalName().equals(selected)) ;
					alist.add(json) ;
				}
				result.put("analyzer", alist);

				result.put(Def.Indexer.IndexAnalyzer, readNode.property(Def.Indexer.IndexAnalyzer).asString());
				result.put(Def.Indexer.ApplyStopword , readNode.property(Def.Indexer.ApplyStopword).asBoolean());

				
				return result;
			}
		});
	}
	
//	.postParam(Def.Indexer.IndexAnalyzer, MyKoreanAnalyzer.class.getCanonicalName())
//	.postParam("stopwords", "bleu jin hero")
//	.postParam("applystopword", "true")
//	.postParam(Def.Indexer.QueryAnalyzer,  MyKoreanAnalyzer.class.getCanonicalName())
	
	@POST
	@Path("/{iid}/defined")
	@Produces(MediaType.TEXT_PLAIN)
	public String defineIndexer(@PathParam("iid") final String iid, @FormParam("indexanalyzer") final String ianalyzerName, 
				@DefaultValue("") @FormParam("stopword") final String stopwords, @DefaultValue("false") @FormParam("applystopword") final boolean applystopword, 
				@FormParam("queryanalyzer") final String qanalyzerName) throws IOException, InterruptedException, ExecutionException{
		return rsession.tran(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				String[] words = StringUtil.split(stopwords, "\\s+");
				wsession.pathBy(fqnBy(iid))
					.property(Def.Indexer.IndexAnalyzer, ianalyzerName)
					.property(Def.Indexer.StopWord,  words).property(Def.Indexer.ApplyStopword, applystopword)
					.property(Def.Indexer.QueryAnalyzer, qanalyzerName);
				return "defined indexer : " + iid;
			}
		}).get() ;
		
	}
	
	@GET
	@Path("/{iid}/defined")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewDefined(@PathParam("iid") String iid){
		return rsession.ghostBy(fqnBy(iid)).transformer(new Function<ReadNode, JsonObject>(){
			@Override
			public JsonObject apply(ReadNode target) {
				JsonObject result = new JsonObject() ;
				result
					.put("info", rsession.ghostBy("/menus/indexers").property("defined").asString())
					.put(Def.Indexer.IndexAnalyzer, target.property(Def.Indexer.IndexAnalyzer).asString())
					.put(Def.Indexer.StopWord, target.property(Def.Indexer.StopWord).asString())
					.put(Def.Indexer.ApplyStopword, target.property(Def.Indexer.ApplyStopword).asBoolean())
					.put(Def.Indexer.QueryAnalyzer, target.property(Def.Indexer.QueryAnalyzer).asString())
					;
				
				JsonArray iarray = new JsonArray() ;
				List<Class<? extends Analyzer>> ilist = AnalysisWeb.analysis() ;
				String iselected = target.property(Def.Indexer.IndexAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()) ;
				for (Class<? extends Analyzer> clz : ilist) {
					JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", clz.getCanonicalName().equals(iselected)) ;
					iarray.add(json) ;
				}
				result.put("index_analyzer", iarray);				

				
				JsonArray qarray = new JsonArray() ;
				List<Class<? extends Analyzer>> alist = AnalysisWeb.analysis() ;
				String qselected = target.property(Def.Indexer.QueryAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()) ;
				for (Class<? extends Analyzer> clz : alist) {
					JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", clz.getCanonicalName().equals(qselected)) ;
					qarray.add(json) ;
				}
				result.put("query_analyzer", qarray);				

				return result;
			}
			
		}) ;
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
//	@GET
//	@Path("/{iid}/schema")
//	@Produces(MediaType.APPLICATION_JSON)
//	public JsonObject schemaList(@PathParam("iid") String iid){
//		
//		JsonObject result = new JsonObject() ;
//		result.put("info", rsession.ghostBy("/menus/indexers").property("schema").asString()) ;
//		JsonArray schemas = rsession.ghostBy(Schema.path(iid)).children().eachNode(new ReadChildrenEach<JsonArray>() {
//			@Override
//			public JsonArray handle(ReadChildrenIterator iter) {
//				JsonArray result = new JsonArray() ;
//				for(ReadNode node : iter){
//					StringBuilder options = new StringBuilder() ;
//					options.append(node.property(Schema.Store).asBoolean() ? "Store:Yes" : "Store:No") ;   
//					options.append(node.property(Schema.Analyze).asBoolean() ? ", Analyze:Yes" : ", Analyze:No") ;   
//					options.append(", Boost:" + StringUtil.defaultIfEmpty(node.property(Schema.Boost).asString(), "1.0")) ;
//					options.append(StringUtil.equals(Def.SchemaType.MANUAL,node.property(Schema.SchemaType).asString()) ? ", Analyzer:" + node.property(Schema.Analyzer).asString() : "") ;
//
//					result.add(new JsonObject().put("schemaid", node.fqn().name()).put(Schema.SchemaType, node.property(Schema.SchemaType).asString()).put("options", options.toString())) ;
//				}
//				return result;
//			}
//		}) ;
//		
//		JsonArray iarray = new JsonArray() ;
//		List<Class<? extends Analyzer>> ilist = AnalysisWeb.analysis() ;
//		for (Class<? extends Analyzer> clz : ilist) {
//			JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", false) ;
//			iarray.add(json) ;
//		}
//		result.put("index_analyzer", iarray);
//		
//		result.put("schemas", schemas) ;
//		return result ;
//	}
	
	@GET
	@Path("/{iid}/schema")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject listSchema(@PathParam("iid") String iid){
		JsonArray schemas = rsession.ghostBy(Schema.path(iid)).children().eachNode(new ReadChildrenEach<JsonArray>() {
			@Override
			public JsonArray handle(ReadChildrenIterator iter) {
				JsonArray result = new JsonArray() ;
				for(ReadNode node : iter){
					StringBuilder options = new StringBuilder() ;
					options.append(node.property(Schema.Store).asBoolean() ? "Store:Yes" : "Store:No") ;   
					options.append(node.property(Schema.Analyze).asBoolean() ? ", Analyze:Yes" : ", Analyze:No") ;   
					options.append(", Boost:" + StringUtil.defaultIfEmpty(node.property(Schema.Boost).asString(), "1.0")) ;
					options.append(StringUtil.equals(Def.SchemaType.MANUAL,node.property(Schema.SchemaType).asString()) ? ", Analyzer:" + node.property(Schema.Analyzer).asString() : "") ;

					result.add(new JsonArray().adds(node.fqn().name(), node.property(Schema.SchemaType).asString(), options.toString())) ;
				}
				return result;
			}
		}) ;
		
		
		JsonArray iarray = new JsonArray() ;
		List<Class<? extends Analyzer>> ilist = AnalysisWeb.analysis() ;
		for (Class<? extends Analyzer> clz : ilist) {
			JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", false) ;
			iarray.add(json) ;
		}
		
		return new JsonObject()
				.put("info", rsession.ghostBy("/menus/indexers").property("schema").asString())
				.put("index_analyzer", iarray)
				.put("schemaName", JsonParser.fromString("[{'title':'SchemaId'},{'title':'Type'},{'title':'Option'}]").getAsJsonArray())
				.put("data", schemas) ;
	}
	
	
	@POST
	@Path("/{iid}/schema")
	@Produces(MediaType.TEXT_PLAIN)
	public String addSchema(@PathParam("iid") final String iid, @FormParam("schemaid") final String schemaid, @FormParam("schematype") final String schematype,
			@FormParam("analyzer") final String analyzer, @FormParam("analyze") final boolean analyze, @FormParam("store") final boolean store, 
			@DefaultValue("1.0") @FormParam("boost") final String boost){
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(Schema.path(iid, schemaid))
					.property(Schema.SchemaType, schematype)
					.property(Schema.Analyzer, "manual".equals(schematype) ? analyzer : "").property(Schema.Analyze, "manual".equals(schematype) ? analyze : false).property(Schema.Store, "manual".equals(schematype) ? store : false)
					.property(Schema.Boost, Double.valueOf(StringUtil.defaultIfEmpty(boost, "1.0"))) ;
				return null;
			}
		}) ;
		
		return "created schema " + schemaid ;
	}
	

	@DELETE
	@Path("/{iid}/schema/{schemaid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String removeSchema(@PathParam("iid") final String iid, @PathParam("schemaid") final String schemaid){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(Schema.path(iid, schemaid)).removeSelf() ;
				return null;
			}
		}) ;
		
		return "removed schema " + schemaid ;
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
	public String indexJson(@PathParam("iid") final String iid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request){

		Indexer indexer = imanager.index(iid).newIndexer() ;
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				
				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				
				JsonObject json = JsonObject.fromString(documents) ;
				WriteDocument wdoc = isession.newDocument(StringUtil.defaultIfEmpty(json.asString("id"), new ObjectId().toString() ) ) ;
				wdoc.add(json) ;
				
				if (overwrite) isession.updateDocument(wdoc) ;
				else isession.insertDocument(wdoc) ;
				return null;
			}

		}) ;
		
		return "1 indexed" ;
	}

	// -- use test
	FieldIndexingStrategy createIndexStrategy(String iid) {
		return imanager.fieldIndexStrategy(rsession, iid) ;
	}

	
	@POST
	@Path("/{iid}/index.jarray")
	@Produces(MediaType.TEXT_PLAIN)
	public String indexJarray(@PathParam("iid") final String iid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request){
		Indexer indexer = imanager.index(iid).newIndexer() ;
		final JsonArray jarray = JsonParser.fromString(documents).getAsJsonArray() ;
		
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				
				for (int i=0 ; i <jarray.size() ; i++) {
					JsonObject json = jarray.get(i).getAsJsonObject() ;
					String idVlaue = StringUtil.isBlank(json.asString("id")) ? new ObjectId().toString() : json.asString("id") ;
					WriteDocument wdoc = isession.newDocument(idVlaue) ;
					wdoc.add(json) ;
					
					Void v = overwrite ? wdoc.updateVoid() : wdoc.insertVoid() ;
				}
				
				return null;
			}
		}) ;
		
		return jarray.size() + " indexed" ;
	}
	
	@POST
	@Path("/{iid}/index.csv")
	@Produces(MediaType.TEXT_PLAIN)
	public String indexCsv(@PathParam("iid") final String iid, @FormParam("documents") final String documents, 
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
				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				
				while((fields = creader.readLine()) != null){
					Map<String, String> map = MapUtil.newMap() ;
					for (int i = 0 ; i < headers.length ; i++) {
						map.put(headers[i], i < fields.length ? fields[i] : "") ;
					}
					
					count++ ;
					WriteDocument wdoc = isession.newDocument(StringUtil.defaultIfEmpty(map.get("id"), new ObjectId().toString())).add(map) ;
					
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
	@Path("/{iid}/browsing")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject browsing(@PathParam("iid") String iid, @QueryParam("searchQuery") String query, @Context HttpRequest request) throws IOException, ParseException{
		final JsonObject result = new JsonObject() ;

		result.put("info", rsession.ghostBy("/menus/indexers").property("browsing").asString()) ;

		SearchResponse response = imanager.index(iid).newSearcher().createRequest(query).offset(101).find() ;
		return response.transformer(new Function<TransformerKey, JsonObject>(){
			@Override
			public JsonObject apply(TransformerKey tkey) {
				List<Integer> docs = tkey.docs();
				SearchRequest request = tkey.request();
				ISearchable searcher = tkey.searcher();
				
				try {
					
					Set<String> fnames = SetUtil.newOrdereddSet() ;
					fnames.add("id") ;
					for (int did : docs) {
						ReadDocument rdoc = searcher.doc(did, request);
						for(String fname : rdoc.fieldNames()){
							fnames.add(fname) ;
						}
					} // define fnames
					
					JsonArray schemaNames = new JsonArray();
					for (String fname : fnames) {
						schemaNames.add(new JsonObject().put("title", fname)) ;
					}
					result.put("schemaName", schemaNames) ;
					
					
					JsonArray dataArray = new JsonArray() ;
					for (int did : docs) {
						ReadDocument rdoc = searcher.doc(did, request);
						JsonArray rowArray = new JsonArray() ;
						for(String fname : fnames){
							rowArray.add(new JsonPrimitive(rdoc.asString(fname, ""))) ;
						}
						dataArray.add(rowArray);
					}
					result.put("data", dataArray) ;

					return result;
				} catch (IOException ex) {
					result.put("exception", ex.getMessage());
					return result;
				}
			}
		}) ;
	}
	
	@POST
	@Path("/{iid}/browsing")
	public String removeIndexRow(@PathParam("iid") String iid, @DefaultValue("") @FormParam("indexIds") final String indexIds){
		Integer count = imanager.index(iid).newIndexer().index(new IndexJob<Integer>() {
			@Override
			public Integer handle(IndexSession isession) throws Exception {
				String[] ids = StringUtil.split(indexIds, ",") ;
				for (String id : ids) {
					isession.deleteDocument(id) ;	
				}
				return ids.length;
			}
		}) ;
		
		return count + " removed" ;
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







