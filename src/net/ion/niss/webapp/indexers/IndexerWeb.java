package net.ion.niss.webapp.indexers;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.Source;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jboss.resteasy.spi.HttpRequest;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.ReadStream;
import net.bleujin.rcraken.WriteNode;
import net.bleujin.searcher.common.FieldIndexingStrategy;
import net.bleujin.searcher.common.IKeywordField;
import net.bleujin.searcher.common.ReadDocument;
import net.bleujin.searcher.common.WriteDocument;
import net.bleujin.searcher.index.IndexJob;
import net.bleujin.searcher.index.IndexSession;
import net.bleujin.searcher.reader.InfoHandler;
import net.bleujin.searcher.search.SearchRequest;
import net.bleujin.searcher.search.SearchResponse;
import net.bleujin.searcher.search.SearchSession;
import net.bleujin.searcher.search.TransformerSearchKey;
import net.ion.framework.parse.gson.GsonBuilder;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.NissServer;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.CSVStreamOut;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.IndexSchema;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.niss.webapp.common.JsonStreamOut;
import net.ion.niss.webapp.common.SourceStreamOut;
import net.ion.niss.webapp.common.Trans;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.radon.core.ContextParam;
import net.ion.radon.util.csv.CsvReader;

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
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonArray listIndexer() {
		ReadStream children = rsession.pathBy("/indexers").children().stream().ascending(Def.Indexer.Created);

		return children.transform(new Function<Iterable<ReadNode>, JsonArray>() {
			@Override
			public JsonArray apply(Iterable<ReadNode> iter) {
				JsonArray result = new JsonArray();
				for (ReadNode node : iter) {
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
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String create(@PathParam("iid") final String iid, @Context HttpRequest req) throws Exception {

		return rsession.tranSync(wsession -> {
			if (wsession.readSession().exist(fqnBy(iid))) return "already exist : " + iid ;
			wsession.pathBy(fqnBy(iid)).property(Def.Indexer.Created, System.currentTimeMillis()).merge();
			return "created " + iid;
		});
	}
	
	
	// remove 
	@DELETE
	@Path("/{iid}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String removeIndexer(@PathParam("iid") final String iid){
		rsession.tran(wsession -> {
			WriteNode found = wsession.pathBy(fqnBy(iid));

			JsonObject decent = found.toReadNode().transformer(Trans.DECENT) ;
			StringBuilder sb = new StringBuilder();
			new GsonBuilder().setPrettyPrinting().create().toJson(decent, sb) ;
			
			FileUtil.forceWriteUTF8(new File(Webapp.REMOVED_DIR, "indexer." + iid + ".bak"), sb.toString()) ;
			
			found.removeSelf() ;
		}) ;
		
		return "removed " + iid;
	}
	
	

	// --- overview
	@GET
	@Path("/{iid}/status")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewStatus(@PathParam("iid") String iid) throws IOException{
		 return imanager.index(iid).info(new InfoHandler<JsonObject>() {
				@Override
				public JsonObject view(IndexReader ireader, Directory dir) throws IOException {
					JsonObject json = new JsonObject() ;
					
					json.put("Max Doc", ireader.maxDoc()) ;
					json.put("Nums Docs", ireader.numDocs()) ;
					json.put("Deleted Docs",  ireader.numDeletedDocs()) ;
//					json.put("Version", dir.getVersion()) ;
//					json.put("Segment Count", dir.getIndexCommit().getSegmentCount()) ;
//					json.put("Current", dir.isCurrent()) ;
					
					return json;
				}
			});
	}

	
	@GET
	@Path("/{iid}/dirInfo")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewDirInfo(@PathParam("iid") final String iid, @ContextParam("net.ion.niss.NissServer") final NissServer nserver) throws IOException{
		return imanager.index(iid).info(new InfoHandler<JsonObject>() {
			@Override
			public JsonObject view(IndexReader ireader, Directory dir) throws IOException {
				JsonObject json = new JsonObject() ;
				
				
//				json.put("LockFactory", dir.directory().getLockFactory().getClass().getCanonicalName()) ;
				json.put("Diretory Impl", dir.getClass().getCanonicalName()) ;
				if (dir instanceof FSDirectory){
					json.put("FileStore Path", ((FSDirectory)dir).getDirectory().toUri().toString()) ;
				} else {
					json.put("FileStore Path", nserver.config().repoConfig().indexHomeDir() + "/" + iid) ;
				}
				
				return json;
			}
		});
	}
	
	
	@GET
	@Path("/{iid}/overview")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject overview(@PathParam("iid") final String iid, @ContextParam("net.ion.niss.NissServer") final NissServer nserver) throws IOException{
		
		return imanager.index(iid).info(new InfoHandler<JsonObject>() {
			@Override
			public JsonObject view(IndexReader ireader, Directory dir) throws IOException {
				JsonObject result = new JsonObject() ;

				result.put("info", rsession.pathBy( "/indexers/" + iid + "/info").property("overview").asString()) ;
				
				JsonObject status = new JsonObject() ;
				status.put("Max Doc", ireader.maxDoc()) ;
				status.put("Nums Docs", ireader.numDocs()) ;
				status.put("Deleted Docs",  ireader.numDeletedDocs()) ;
//				status.put("Version", ireader.getVersion()) ;
//				status.put("Segment Count", dir.getIndexCommit().getSegmentCount()) ;
//				status.put("Current", dir.isCurrent()) ;
				result.add("status", status);
				
				JsonObject dirInfo = new JsonObject() ;
//				dirInfo.put("LockFactory", dir.getLockFactory().getClass().getCanonicalName()) ;
				dirInfo.put("Diretory Impl", dir.getClass().getCanonicalName()) ;
				if (dir instanceof FSDirectory){
					dirInfo.put("FileStore Path", ((FSDirectory)dir).getDirectory().toUri().toString()) ;
				}else {
					dirInfo.put("FileStore Path", nserver.config().repoConfig().indexHomeDir() + iid) ;
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
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String defineIndexer(@PathParam("iid") final String iid, @FormParam("indexanalyzer") final String ianalyzerName, 
				@DefaultValue("") @FormParam("stopword") final String stopwords, @DefaultValue("false") @FormParam("applystopword") final boolean applystopword, 
				@FormParam("queryanalyzer") final String qanalyzerName) throws IOException, InterruptedException, ExecutionException{
		return rsession.tran(wsession -> {
			wsession.pathBy(fqnBy(iid))
				.property(Def.Indexer.IndexAnalyzer, ianalyzerName)
				.property(Def.Indexer.StopWord,  stopwords).property(Def.Indexer.ApplyStopword, applystopword)
				.property(Def.Indexer.QueryAnalyzer, qanalyzerName).merge();
			return "defined indexer : " + iid;
		}).get() ;
		
	}
	
	@GET
	@Path("/{iid}/defined")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewDefined(@PathParam("iid") String iid){
		return rsession.pathBy(fqnBy(iid)).transformer(new Function<ReadNode, JsonObject>(){
			@Override
			public JsonObject apply(ReadNode target) {
				JsonObject result = new JsonObject() ;
				result
					.put("info", rsession.pathBy("/indexers/" + iid + "/info").property("define").asString())
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
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String updateField(@PathParam("iid") final String iid, @FormParam("field") final String field, @FormParam("content") final String value) throws Exception{
		rsession.tran(wsession -> {
			wsession.pathBy(fqnBy(iid)).property(field, value).merge();
		}) ;
		return value.trim() ;
	}
	
	
	@GET
	@Path("/{iid}/schema")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject listSchema(@PathParam("iid") String iid){
		JsonArray schemas = rsession.pathBy(IndexSchema.path(iid)).children().stream().transform((iter) -> {
				JsonArray result = new JsonArray() ;
				for(ReadNode node : iter){
					StringBuilder options = new StringBuilder() ;
					options.append(node.property(IndexSchema.Store).asBoolean() ? "Store:Yes" : "Store:No") ;   
					options.append(node.property(IndexSchema.Analyze).asBoolean() ? ", Analyze:Yes" : ", Analyze:No") ;   
					options.append(", Boost:" + StringUtil.defaultIfEmpty(node.property(IndexSchema.Boost).asString(), "1.0")) ;
					options.append(StringUtil.equals(Def.SchemaType.MANUAL,node.property(IndexSchema.SchemaType).asString()) ? ", Analyzer:" + node.property(IndexSchema.Analyzer).asString() : "") ;

					result.add(new JsonArray().adds(node.fqn().name(), node.property(IndexSchema.SchemaType).asString(), options.toString())) ;
				}
				return result;
		}) ;
		
		
		JsonArray iarray = new JsonArray() ;
		List<Class<? extends Analyzer>> ilist = AnalysisWeb.analysis() ;
		for (Class<? extends Analyzer> clz : ilist) {
			JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", false) ;
			iarray.add(json) ;
		}
		
		return new JsonObject()
				.put("info", rsession.pathBy("/indexers/" + iid + "/info").property("schema").asString())
				.put("index_analyzer", iarray)
				.put("schemaName", JsonParser.fromString("[{'title':'SchemaId'},{'title':'Type'},{'title':'Option'}]").getAsJsonArray())
				.put("data", schemas) ;
	}
	
	
	@POST
	@Path("/{iid}/schema")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String addSchema(@PathParam("iid") final String iid, @FormParam("schemaid") final String schemaid, @FormParam("schematype") final String schematype,
			@FormParam("analyzer") final String analyzer, @DefaultValue("false") @FormParam("analyze") final boolean analyze, @DefaultValue("false") @FormParam("store") final boolean store, 
			@DefaultValue("1.0") @FormParam("boost") final String boost){
		
		rsession.tran(wsession -> {
			wsession.pathBy(IndexSchema.path(iid, schemaid))
				.property(IndexSchema.SchemaType, schematype)
				.property(IndexSchema.Analyzer, "manual".equals(schematype) ? analyzer : "").property(IndexSchema.Analyze, analyze).property(IndexSchema.Store, store)
				.property(IndexSchema.Boost, Double.valueOf(StringUtil.defaultIfEmpty(boost, "1.0"))).merge(); ;
		}) ;
		
		return "created schema " + schemaid ;
	}
	

	@DELETE
	@Path("/{iid}/schema/{schemaid}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String removeSchema(@PathParam("iid") final String iid, @PathParam("schemaid") final String schemaid){
		rsession.tran(wsession -> {
			wsession.pathBy(IndexSchema.path(iid, schemaid)).removeSelf() ;
		}) ;
		
		return "removed schema " + schemaid ;
	}
	
	
	
	// --- index 
	
	@GET
	@Path("/{iid}/index")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public JsonObject indexView(@PathParam("iid") String iid){
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.pathBy("/indexers/" + iid + "/info").property("index").asString()) ;
		return result ;
	}
	
	
	@POST
	@Path("/{iid}/index.json")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String indexJson(@PathParam("iid") final String iid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") int within, @DefaultValue("1.0") @FormParam("boost") final float boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request) throws IOException{

		final SchemaInfos sinfos = SchemaInfos.create(rsession.pathBy(IndexSchema.path(iid)).children()) ;

		
		imanager.index(iid).index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				
//				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				
				JsonObject json = JsonObject.fromString(documents) ;
				if (! json.has("id")) json.put("id", new ObjectId().toString()) ;
				
				WriteDocument wdoc = isession.newDocument(json.asString("id")) ;
//				wdoc.boost(boost) ;
				
				sinfos.addFields(wdoc, json) ;
//				wdoc.add(json) ;
				
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
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String indexJarray(@PathParam("iid") final String iid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") final int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request) throws IOException{
		final JsonArray jarray = JsonParser.fromString(documents).getAsJsonArray() ;
		final SchemaInfos sinfos = SchemaInfos.create(rsession.pathBy(IndexSchema.path(iid)).children()) ;
		
		imanager.index(iid).index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
//				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				
				for (int i=0 ; i <jarray.size() ; i++) {
					JsonObject json = jarray.get(i).getAsJsonObject() ;
					if (! json.has("id")) json.put("id", new ObjectId().toString()) ;
					
					String idVlaue = json.asString("id");
					WriteDocument wdoc = isession.newDocument(idVlaue) ;
					
					sinfos.addFields(wdoc, json);
//					wdoc.add(json) ;
					
					Void v = overwrite ? wdoc.updateVoid() : wdoc.insertVoid() ;
					if (i != 0 && i % within == 0) isession.continueUnit() ;
				}
				
				return null;
			}
		}) ;
		
		return jarray.size() + " indexed" ;
	}
	
	@POST
	@Path("/{iid}/index.csv")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String indexCsv(@PathParam("iid") final String iid, @FormParam("documents") final String documents, 
			@DefaultValue("1000") @FormParam("within") final int within, @DefaultValue("1.0") @FormParam("boost") double boost, @FormParam("overwrite") final boolean overwrite,
			@Context HttpRequest request) throws IOException{
		final CsvReader creader = new CsvReader(new StringReader(documents)) ;
		final String[] headers = creader.readLine() ;
		final SchemaInfos sinfos = SchemaInfos.create(rsession.pathBy(IndexSchema.path(iid)).children()) ;
		
		int sum = imanager.index(iid).index(new IndexJob<Integer>() {
			@Override
			public Integer handle(IndexSession isession) throws Exception {
				String[] fields ;
				int count = 0 ;
//				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				
				while((fields = creader.readLine()) != null){
					Map<String, String> map = MapUtil.newMap() ;
					for (int i = 0 ; i < headers.length ; i++) {
						map.put(headers[i], i < fields.length ? fields[i] : "") ;
					}
					if (! map.containsKey("id")) map.put("id", new ObjectId().toString()) ;
					
					WriteDocument wdoc = isession.newDocument(map.get("id"));
					sinfos.addFields(wdoc, map);
					
					if (overwrite) isession.updateDocument(wdoc) ;
					else isession.insertDocument(wdoc) ;
					
					if (count != 0 && count % within == 0) isession.continueUnit() ;
					count++ ;
				}
				return count;
			}
		}) ;
		
		return sum + " indexed" ;
	}
	
	
	
	// --- query
	@GET
	@Path("/{iid}/query")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject query(@PathParam("iid") String iid) throws IOException{
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.pathBy("/indexers/" +  iid + "/info").property("query").asString()) ;
		return result ;
	}
	
	@GET
	@Path("/{iid}/query.json")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public StreamingOutput jquery(@PathParam("iid") String iid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		SearchResponse sresponse = imanager.index(iid).newSearcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;

		JsonObject result = sresponse.transformer(Responses.toJson(map, sresponse)) ;
		return new JsonStreamOut(result, indent) ;
	}
	

	@GET
	@Path("/{iid}/query.xml")
	@Produces(ExtMediaType.APPLICATION_XML_UTF8)
	public StreamingOutput xquery(@PathParam("iid") String iid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
		
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		SearchResponse sresponse = imanager.index(iid).newSearcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;
		
		Source result = sresponse.transformer(Responses.toXMLSource(map, sresponse));
		return new SourceStreamOut(result, indent) ;
	}
	

	@GET
	@Path("/{iid}/query.csv")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public StreamingOutput cquery(@PathParam("iid") String iid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset, 
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException{
		
		MultivaluedMap<String, String> map = request.getUri().getQueryParameters() ;
		SearchResponse sresponse = imanager.index(iid).newSearcher().createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find() ;
		
		return new CSVStreamOut(sresponse) ;
	}
	

	@GET
	@Path("/{iid}/browsing")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject browsing(@PathParam("iid") String iid, @QueryParam("searchQuery") String query, @Context HttpRequest request) throws IOException, ParseException{
		final JsonObject result = new JsonObject() ;

		result.put("info", rsession.pathBy("/indexers/" + iid + "/info").property("browsing").asString()) ;
		final Set<String> fnames = SetUtil.newOrdereddSet() ;
		fnames.add("id") ;
		fnames.addAll(rsession.pathBy("/indexers/" + iid + "/schema").childrenNames()) ;

		SearchResponse response = imanager.index(iid).newSearcher().createRequest(query).offset(101).find() ;
		return response.transformer(new com.google.common.base.Function<TransformerSearchKey, JsonObject>(){
			@Override
			public JsonObject apply(TransformerSearchKey tkey) {
				List<Integer> docs = tkey.docs();
				SearchRequest request = tkey.request();
				SearchSession searcher = tkey.session();
				
				try {
					
					JsonArray schemaNames = new JsonArray();
					for (String fname : fnames) {
						schemaNames.add(new JsonObject().put("title", fname)) ;
					}
					result.put("schemaName", schemaNames) ;
					
					
					JsonArray dataArray = new JsonArray() ;
					for (int did : docs) {
						ReadDocument rdoc = searcher.readDocument(did, request);
						JsonArray rowArray = new JsonArray() ;
						for(String fname : fnames){
							if ("id".equals(fname)) rowArray.add(new JsonPrimitive(rdoc.reserved(IKeywordField.DocKey))) ;
							else rowArray.add(new JsonPrimitive(rdoc.asString(fname, ""))) ;
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
	public String removeIndexRow(@PathParam("iid") String iid, @DefaultValue("") @FormParam("indexIds") final String indexIds) throws IOException{
		Integer count = imanager.index(iid).index(new IndexJob<Integer>() {
			@Override
			public Integer handle(IndexSession isession) throws Exception {
				String[] ids = StringUtil.split(indexIds, ",") ;
				for (String id : ids) {
					isession.deleteById(id) ;	
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
	
	
	
	private String fqnBy(String iid) {
		return "/indexers/" + IdString.create(iid).idString();
	}

}







