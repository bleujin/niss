package net.ion.niss.webapp.searchers;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.Source;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.ReadStream;
import net.bleujin.rcraken.WriteNode;
import net.bleujin.searcher.Searcher;
import net.bleujin.searcher.common.IKeywordField;
import net.bleujin.searcher.common.ReadDocument;
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
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.CSVStreamOut;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.SearchSchema;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.niss.webapp.common.JsonStreamOut;
import net.ion.niss.webapp.common.SourceStreamOut;
import net.ion.niss.webapp.common.Trans;
import net.ion.niss.webapp.indexers.Responses;
import net.ion.niss.webapp.indexers.SearchManager;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.niss.webapp.util.WebUtil;
import net.ion.radon.core.ContextParam;

@Path("/searchers")
public class SearcherWeb implements Webapp {

	private ReadSession rsession;
	private SearchManager smanager;
	private QueryTemplateEngine qengine;
	private PopularQueryEntry pqentry;

	public SearcherWeb(@ContextParam(REntry.EntryName) REntry rentry, @ContextParam(QueryTemplateEngine.EntryName) QueryTemplateEngine qengine, @ContextParam(PopularQueryEntry.EntryName) PopularQueryEntry pqentry) throws IOException {
		this.rsession = rentry.login();
		this.smanager = rentry.searchManager();
		this.qengine = qengine ;
		this.pqentry = pqentry;
	}

	@GET
	@Path("")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonArray listSection() {
		ReadStream children = rsession.pathBy("/searchers").children().stream().ascending(Def.Searcher.Created);

		return children.transform(new Function<Iterable<ReadNode>, JsonArray>() {
			@Override
			public JsonArray apply(Iterable<ReadNode> iter) {
				JsonArray result = new JsonArray();
				for (ReadNode node : iter) {
					result.add(new JsonObject().put("sid", node.fqn().name()).put("name", node.fqn().name()));
				}
				return result;
			}
		});
	}

	// create section

	@POST
	@Path("/{sid}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String create(@PathParam("sid") final String sid) throws Exception {
		return rsession.tranSync(wsession -> {
			if (wsession.exist(fqnBy(sid))) return "already exist : " + sid ;
			wsession.pathBy(fqnBy(sid)).property(Def.Searcher.Created, System.currentTimeMillis()).merge();
			return "created " + sid;
		});
	}

	@DELETE
	@Path("/{sid}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String removeSearch(@PathParam("sid") final String sid) {
		rsession.tran(wsession -> {
			WriteNode found = wsession.pathBy(fqnBy(sid));
			JsonObject decent = found.toReadNode().transformer(Trans.DECENT) ;
			StringBuilder sb = new StringBuilder();
			new GsonBuilder().setPrettyPrinting().create().toJson(decent, sb) ;
			
			FileUtil.forceWriteUTF8(new File(Webapp.REMOVED_DIR, "searcher." + sid + ".bak"), sb.toString()) ;

			found.removeSelf() ;
		});

		return "removed " + sid;
	}
	
	
	@GET
	@Path("/{sid}/overview")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewOverview(@PathParam("sid") final String sid){
		
		// wsession.pathBy("/searchlogs/" + query.hashCode()).property("query", query).property('found', response.totalCount()).property('time', System.currentTimeMillis()).increase('count') ;
		Function<Iterable<ReadNode>, JsonArray> makeJson = new Function<Iterable<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterable<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				for (ReadNode node : iter) {
					result.add(new JsonObject().put("query", node.property("query").asString()).put("count", node.property("count").defaultValue(1)).put("time", node.property("time").asLong())) ; 
				}
				return result;
			}
		} ;
		
		JsonArray recent = rsession.pathBy("/searchlogs/" + sid).children().stream().descending("time").limit(10).transform(makeJson) ;
		JsonArray popular = rsession.pathBy("/searchlogs/" + sid).children().stream().descending("count").limit(10).transform(makeJson) ;
		
		return new JsonObject().put("info", rsession.pathBy("/searchers/" + sid + "/info").property("overview").asString())
			.put("recent", recent)
			.put("popular", popular) ;
	}
	
	
	@GET
	@Path("/{sid}/popularquery")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewPopularQuery(@PathParam("sid") final String sid) throws ExecutionException{
		ReadNode found = rsession.pathBy(fqnBy(sid, "popularquery"));
		String qtemplate = found.property(Def.Searcher.Popular.Template).defaultValue(pqentry.DFT_TEMPLATE) ;
		
		String transformd = pqentry.result(sid) ;
		
		return new JsonObject().put("qtemplate", JsonParser.fromString(qtemplate)).put("transformed", transformd).put("dayrange", found.property("dayrange").defaultValue(3)) ;
	}
	

	@POST
	@Path("/{sid}/popularquery")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public String editPopularQuery(@PathParam("sid") final String sid, final @FormParam("ptemplate") String mypopularTemplate, @DefaultValue("3") @FormParam("dayrange") final int dayRange){
		rsession.tran(wsession -> {
			wsession.pathBy(fqnBy(sid, "popularquery")).property(Def.Searcher.Popular.Template, mypopularTemplate).property(Def.Searcher.Popular.DayRange, dayRange).merge();
		}) ;
		
		pqentry.invalidate(sid) ;
		
		return "popular query edited" ;
	}
	

	
	@GET
	@Path("/{sid}/define")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewSearcher(@PathParam("sid") final String sid) {

		final String[] colNames = rsession.pathBy("/indexers").childrenNames().toArray(new String[0]);

		return rsession.pathBy(fqnBy(sid)).transformer(new Function<ReadNode, JsonObject>() {
			@Override
			public JsonObject apply(ReadNode node) {
				JsonObject result = new JsonObject()
						.put("info", rsession.pathBy("/searchers/" + sid + "/info").property("define").asString()).put("indexers", colNames)
						.put(Def.Searcher.QueryAnalyzer, node.property(Def.Searcher.QueryAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()))
						.put("target", node.property(Def.Searcher.Target).asSet().toArray(new String[0]))
						.put(Def.Searcher.Handler, node.property(Def.Searcher.Handler).asString()).put(Def.Searcher.ApplyHandler, node.property(Def.Searcher.ApplyHandler).asBoolean())
						.put("samples", WebUtil.findSearchHandlers())
						.put(Def.Searcher.StopWord, node.property(Def.Searcher.StopWord).asString()).put(Def.Searcher.ApplyStopword, node.property(Def.Searcher.ApplyStopword).asBoolean());

				JsonArray qarray = new JsonArray();
				List<Class<? extends Analyzer>> ilist = AnalysisWeb.analysis();
				String iselected = node.property(Def.Indexer.QueryAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName());
				for (Class<? extends Analyzer> clz : ilist) {
					JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", clz.getCanonicalName().equals(iselected));
					qarray.add(json);
				}
				result.put("query_analyzer", qarray);

				return result;
			}
		});
	}

	// define section
	@POST
	@Path("/{sid}/define")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String defineSearcher(@PathParam("sid") final String sid, @FormParam("target") final String target, @Context HttpRequest request, @FormParam("queryanalyzer") final String queryAnalyzer, @FormParam("handler") final String handler,
			@DefaultValue("false") @FormParam("applyhandler") final boolean applyHandler, @FormParam("stopword") final String stopword, @DefaultValue("false") @FormParam("applystopword") final boolean applyStopword) {

		final String[] targets = StringUtil.split(target, ",");
		rsession.tran(wsession -> {
			WriteNode found = wsession.pathBy(fqnBy(sid)) ;
			FileUtil.forceWriteUTF8(new File(Webapp.REMOVED_DIR,  sid + ".searcher.handler.bak"), found.property(Def.Searcher.Handler).asString());
			
			wsession.pathBy(fqnBy(sid)).property(Def.Searcher.Target, targets).property(Def.Searcher.QueryAnalyzer, queryAnalyzer).property(Def.Searcher.Handler, handler).property(Def.Searcher.ApplyHandler, applyHandler).property(Def.Searcher.StopWord, stopword)
					.property(Def.Searcher.ApplyStopword, applyStopword).merge();
		});

		return "defined searcher : " + sid;
	}

	@GET
	@Path("/{sid}/samplehandler/{fileName}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String viewSampleHandler(@PathParam("sid") final String lid, @PathParam("fileName") String fileName) throws IOException{
		return WebUtil.viewSearchHandler(fileName) ;
	}
	

	
//	@GET
//	@Path("/{sid}/define.default")
//	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
//	public String defaultHandler(@PathParam("sid") final String sid) throws IOException {
//		return IOUtil.toStringWithClose(new FileInputStream(SEARCH_HANDLER_FILE));
//	}

	// --- schema
	@GET
	@Path("/{sid}/schema")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject listSchema(@PathParam("sid") String sid){
		JsonArray schemas = rsession.pathBy(SearchSchema.path(sid)).children().stream().transform(iter -> {
			JsonArray result = new JsonArray() ;
			for(ReadNode node : iter){
				result.add(new JsonArray().adds(node.fqn().name(), node.property(SearchSchema.Analyzer).asString())) ;
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
				.put("info", rsession.pathBy("/searchers/" + sid + "/info").property("schema").asString())
				.put("query_analyzer", iarray)
				.put("schemaName", JsonParser.fromString("[{'title':'SchemaId'},{'title':'Analyzer'}]").getAsJsonArray())
				.put("data", schemas) ;
	}
	
	
	@POST
	@Path("/{sid}/schema")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String addSchema(@PathParam("sid") final String sid, @FormParam("schemaid") final String schemaid, @FormParam("analyzer") final String analyzer){
		
		rsession.tran(wsession -> {
			wsession.pathBy(SearchSchema.path(sid, schemaid)).property(SearchSchema.Analyzer, analyzer).merge();
		}) ;
		
		return "created search schema " + schemaid ;
	}
	

	@DELETE
	@Path("/{sid}/schema/{schemaid}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String removeSchema(@PathParam("sid") final String sid, @PathParam("schemaid") final String schemaid){
		rsession.tran(wsession -> {
			wsession.pathBy(SearchSchema.path(sid, schemaid)).removeSelf() ;
		}) ;
		
		return "removed schema " + schemaid ;
	}
	
	
	
	
	// --- query
	@GET
	@Path("/{sid}/query")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject query(@PathParam("sid") String sid) throws IOException {
		JsonObject result = new JsonObject();
		result.put("info", rsession.pathBy("/searchers/" + sid + "/info").property("query").asString());
		return result;
	}

	@GET
	@Path("/{sid}/query.json")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public StreamingOutput jquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters();
		SearchResponse sresponse = searchQuery(sid, query, sort, skip, offset, request, map);

		JsonObject result = sresponse.transformer(Responses.toJson(map, sresponse));
		return new JsonStreamOut(result, indent);
	}

	private SearchResponse searchQuery(String sid, String query, String sort, String skip, String offset, HttpRequest request, MultivaluedMap<String, String> map) throws IOException, ParseException {
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getDecodedFormParameters().size() > 0)
			map.putAll(request.getDecodedFormParameters());

		Searcher searcher = smanager.searcher(sid);
		
//		int defaultSkip = searcher.config().attrAsInt("skip", 0) ;
//		int defaultOffset = searcher.config().attrAsInt("offset", 10) ;
//		String defaultSort = searcher.config().attrAsString("sort", "") ;
		
		SearchResponse sresponse = searcher.createRequest(query).sort(StringUtil.coalesce(sort, "")).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find();
		return sresponse;
	}

	@GET
	@Path("/{sid}/query.xml")
	@Produces(ExtMediaType.APPLICATION_XML_UTF8)
	public StreamingOutput xquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters();
		SearchResponse sresponse = searchQuery(sid, query, sort, skip, offset, request, map);

		Source result = sresponse.transformer(Responses.toXMLSource(map, sresponse));
		return new SourceStreamOut(result, indent);
	}

	@GET
	@Path("/{sid}/query.csv")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public StreamingOutput cquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters();
		SearchResponse sresponse = searchQuery(sid, query, sort, skip, offset, request, map);

		return new CSVStreamOut(sresponse);
	}

	@GET
	@Path("/{sid}/query.template")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public Response tquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @QueryParam("ishtml") boolean ishtml, @Context HttpRequest request) throws IOException, ParseException {

		try {
			MultivaluedMap<String, String> map = request.getUri().getQueryParameters();
			SearchResponse sresponse = searchQuery(sid, query, sort, skip, offset, request, map);

			String resourceName = fqnBy(sid).toString() + ".template" ;
			StringWriter writer = new StringWriter();
			qengine.merge(resourceName, MapUtil.<String, Object> chainMap().put("response", sresponse).put("params", map).toMap(), writer);

			return Response.ok(writer.toString(), ishtml ? ExtMediaType.TEXT_HTML_UTF8 : ExtMediaType.TEXT_PLAIN_UTF8).build() ;
		} catch (org.apache.velocity.exception.ParseErrorException tex) {
			tex.printStackTrace(); 
			return Response.status(500).entity(tex.getMessage()).build();
		}
	}

	// -- template
	@GET
	@Path("/{sid}/template")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewTemplate(@PathParam("sid") final String sid) {
		JsonObject result = new JsonObject();
		result.put("info", rsession.pathBy("/searchers/" + sid + "/info").property("template").asString());
		result.put("samples", WebUtil.findSearchTemplates()) ;
		result.put("template", rsession.pathBy(fqnBy(sid)).property(Def.Searcher.Template).asString());
		return result;
	}

	@GET
	@Path("/{sid}/sampletemplate/{fileName}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String viewSampleTemplate(@PathParam("sid") final String sid, @PathParam("fileName") String fileName) throws IOException{
		return WebUtil.viewSearchTemplate(fileName) ;
	}
	
//	@GET
//	@Path("/{sid}/template.default")
//	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
//	public String defaultTemplate(@PathParam("sid") final String sid) throws IOException {
//		return IOUtil.toStringWithClose(new FileInputStream(SEARCH_TEMPLATE_FILE));
//	}

	@POST
	@Path("/{sid}/template")
	public String editTemplate(@PathParam("sid") final String sid, @FormParam("template") final String template) {
		rsession.tran(wsession -> {
			WriteNode found = wsession.pathBy(fqnBy(sid));
			FileUtil.forceWriteUTF8(new File(Webapp.REMOVED_DIR,  sid + ".searcher.template.bak"), found.property(Def.Searcher.Template).asString());
			
			found.property(Def.Searcher.Template, template).merge();
		});
		return "modified template : " + sid;
	}

	
	@GET
	@Path("/{sid}/browsing")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject browsing(@PathParam("sid") final String sid, @DefaultValue("") @QueryParam("searchQuery") final String searchQuery, @DefaultValue("101") @QueryParam("offset") final int offset) throws IOException, ParseException{
		
		final SearchResponse response = smanager.searcher(sid).createRequest(searchQuery).offset(offset).find() ;
		final Set<String> fnames = SetUtil.newOrdereddSet() ;
		fnames.add("id") ;
		fnames.addAll(rsession.pathBy("/searchers/" + sid + "/schema").childrenNames()) ;

		
		return response.transformer(new com.google.common.base.Function<TransformerSearchKey, JsonObject>() {
			@Override
			public JsonObject apply(TransformerSearchKey tkey) {
				List<Integer> docs = tkey.docs();
				SearchRequest request = tkey.request();
				SearchSession searcher = tkey.session();

				JsonObject result = JsonObject.create();
				JsonObject header = JsonObject.create();
				result.add("header", header);

				header.put("size", docs.size());
				header.put("total", response.totalCount());
				header.put("skip", request.skip());
				header.put("offset", request.offset());

				header.put("elapsedTime", response.elapsedTime());
				JsonArray jarray = new JsonArray();
				result.put("data", jarray);
				result.put("info", rsession.pathBy("/searchers/" + sid + "/info").property("browsing").asString()) ;

				try {
					
//					Set<String> fnames = SetUtil.newOrdereddSet() ;
//					fnames.add("id") ;
//					for (int did : docs) {
//						ReadDocument rdoc = searcher.doc(did, request);
//						for(String fname : rdoc.fieldNames()){
//							fnames.add(fname) ;
//						}
//					} // define fnames
					
					JsonArray schemaNames = new JsonArray();
					for (String fname : fnames) {
						schemaNames.add(new JsonObject().put("title", fname)) ;
					}
					result.put("schemaName", schemaNames) ;
					
					
					for (int did : docs) {
						ReadDocument rdoc = searcher.readDocument(did, request);
						JsonArray rowArray = new JsonArray() ;
						for(String fname : fnames){
							if ("id".equals(fname)) rowArray.add(new JsonPrimitive(rdoc.reserved(IKeywordField.DocKey))) ;
							else rowArray.add(new JsonPrimitive(rdoc.asString(fname, ""))) ;
						}
						jarray.add(rowArray);
					}

					return result;
				} catch (IOException ex) {
					header.put("exception", ex.getMessage());
					return result;
				}
			}
		});
	}
	
	
	
	private String fqnBy(String sid) {
		return "/searchers/" + IdString.create(sid).idString();
	}

	private String fqnBy(String sid, String child) {
		return "/searchers/" + IdString.create(sid).idString() + "/" + child;
	}

}
