package net.ion.niss.webapp.searchers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.tree.Fqn;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.CSVStreamOut;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.JsonStreamOut;
import net.ion.niss.webapp.common.SourceStreamOut;
import net.ion.niss.webapp.indexers.Responses;
import net.ion.niss.webapp.indexers.SearchManager;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.ISearchable;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.TransformerKey;
import net.ion.radon.core.ContextParam;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

import com.google.common.base.Function;

@Path("/searchers")
public class SearcherWeb implements Webapp {

	private ReadSession rsession;
	private SearchManager smanager;
	private QueryTemplateEngine qengine;

	public SearcherWeb(@ContextParam("rentry") REntry rentry, @ContextParam("qtemplate") QueryTemplateEngine qengine) throws IOException {
		this.rsession = rentry.login();
		this.smanager = rentry.searchManager();
		this.qengine = qengine ;
	}

	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray listSection() {
		ReadChildren children = rsession.ghostBy("/searchers").children();

		return children.transform(new Function<Iterator<ReadNode>, JsonArray>() {
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray();
				while (iter.hasNext()) {
					ReadNode node = iter.next();
					result.add(new JsonObject().put("sid", node.fqn().name()).put("name", node.fqn().name()));
				}
				return result;
			}
		});
	}

	// create section

	@POST
	@Path("/{sid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String create(@PathParam("sid") final String sid) {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(sid)).property("created", System.currentTimeMillis());
				return null;
			}
		});

		return "created " + sid;
	}

	@DELETE
	@Path("/{sid}")
	@Produces(MediaType.TEXT_PLAIN)
	public String removeSearch(@PathParam("sid") final String sid) {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(sid)).removeSelf() ;
				return null;
			}
		});

		return "removed " + sid;
	}


	
	@GET
	@Path("/{sid}/define")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewSection(@PathParam("sid") final String sid) {

		final String[] colNames = rsession.ghostBy("/indexers").childrenNames().toArray(new String[0]);

		return rsession.pathBy(fqnBy(sid)).transformer(new Function<ReadNode, JsonObject>() {
			@Override
			public JsonObject apply(ReadNode node) {
				JsonObject result = new JsonObject().put("info", rsession.ghostBy("/menus/searchers").property("define").asString()).put("indexers", colNames).put(Def.Searcher.QueryAnalyzer, node.property(Def.Searcher.QueryAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()))
						.put("target", node.property(Def.Searcher.Target).asSet().toArray(new String[0])).put(Def.Searcher.Handler, node.property(Def.Searcher.Handler).asString()).put(Def.Searcher.ApplyHandler, node.property(Def.Searcher.ApplyHandler).asBoolean())
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
	@Produces(MediaType.TEXT_PLAIN)
	public String defineSection(@PathParam("sid") final String sid, @FormParam("target") final String target, @Context HttpRequest request, @FormParam("queryanalyzer") final String queryAnalyzer, @FormParam("handler") final String handler,
			@DefaultValue("false") @FormParam("applyhandler") final boolean applyHandler, @FormParam("stopword") final String stopword, @DefaultValue("false") @FormParam("applystopword") final boolean applyStopword) {

		final String[] targets = StringUtil.split(target, ",");
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode wnode = wsession.pathBy(fqnBy(sid)).property(Def.Searcher.Target, targets).property(Def.Searcher.QueryAnalyzer, queryAnalyzer).property(Def.Searcher.Handler, handler).property(Def.Searcher.ApplyHandler, applyHandler).property(Def.Searcher.StopWord, stopword)
						.property(Def.Searcher.ApplyStopword, applyStopword);

				return null;
			}
		});

		return "defined searcher : " + sid;
	}

	@GET
	@Path("/{sid}/define.default")
	@Produces(MediaType.TEXT_PLAIN)
	public String defaultHandler(@PathParam("sid") final String sid) throws IOException {
		return IOUtil.toStringWithClose(getClass().getResourceAsStream("default.handler"));
	}

	// --- query
	@GET
	@Path("/{sid}/query")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject query() throws IOException {
		JsonObject result = new JsonObject();
		result.put("info", rsession.ghostBy("/menus/searchers").property("query").asString());
		return result;
	}

	@GET
	@Path("/{sid}/query.json")
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput jquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters();
		SearchResponse sresponse = searchQuery(sid, query, sort, skip, offset, request, map);

		JsonObject result = sresponse.transformer(Responses.toJson(map, sresponse));
		return new JsonStreamOut(result, indent);
	}

	private SearchResponse searchQuery(String sid, String query, String sort, String skip, String offset, HttpRequest request, MultivaluedMap<String, String> map) throws IOException, ParseException {
		if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getFormParameters().size() > 0)
			map.putAll(request.getFormParameters());

		SearchResponse sresponse = smanager.searcher(sid).createRequest(query).sort(sort).skip(NumberUtil.toInt(skip, 0)).offset(NumberUtil.toInt(offset, 10)).find();
		return sresponse;
	}

	@GET
	@Path("/{sid}/query.xml")
	@Produces(MediaType.APPLICATION_XML)
	public StreamingOutput xquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters();
		SearchResponse sresponse = searchQuery(sid, query, sort, skip, offset, request, map);

		Source result = sresponse.transformer(Responses.toXMLSource(map, sresponse));
		return new SourceStreamOut(result, indent);
	}

	@GET
	@Path("/{sid}/query.csv")
	@Produces(MediaType.TEXT_PLAIN)
	public StreamingOutput cquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> map = request.getUri().getQueryParameters();
		SearchResponse sresponse = searchQuery(sid, query, sort, skip, offset, request, map);

		return new CSVStreamOut(sresponse);
	}

	@GET
	@Path("/{sid}/query.template")
	public String tquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		try {
			MultivaluedMap<String, String> map = request.getUri().getQueryParameters();
			SearchResponse sresponse = searchQuery(sid, query, sort, skip, offset, request, map);

			String resourceName = fqnBy(sid).toString() + ".template" ;
			StringWriter writer = new StringWriter();
			qengine.merge(resourceName, MapUtil.<String, Object> chainMap().put("response", sresponse).put("params", map).toMap(), writer);
			
			return writer.toString() ;
		} catch (org.apache.velocity.exception.ParseErrorException tex) {
			tex.printStackTrace(); 
			return tex.getMessage();
		}
	}

	// -- template
	@GET
	@Path("/{sid}/template")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject viewTemplate(@PathParam("sid") final String sid) {
		JsonObject result = new JsonObject();
		result.put("info", rsession.ghostBy("/menus/searchers").property("template").asString());
		result.put("template", rsession.pathBy(fqnBy(sid)).property(Def.Searcher.Template).asString());
		return result;
	}

	@GET
	@Path("/{sid}/template.default")
	@Produces(MediaType.TEXT_PLAIN)
	public String defaultTemplate(@PathParam("sid") final String sid) throws IOException {
		return IOUtil.toStringWithClose(getClass().getResourceAsStream("default.template"));
	}

	@POST
	@Path("/{sid}/template")
	public String editTemplate(@PathParam("sid") final String sid, @FormParam("template") final String template) {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(sid)).property(Def.Searcher.Template, template);
				return null;
			}
		});
		return "modified template : " + sid;
	}

	
	@GET
	@Path("/{sid}/browsing")
	public JsonObject browsing(@PathParam("sid") final String sid, @DefaultValue("") @QueryParam("searchQuery") final String searchQuery, @DefaultValue("101") @QueryParam("offset") final int offset) throws IOException, ParseException{
		
		final SearchResponse response = smanager.searcher(sid).createRequest(searchQuery).offset(offset).find() ;
		
		return response.transformer(new Function<TransformerKey, JsonObject>() {
			@Override
			public JsonObject apply(TransformerKey tkey) {
				List<Integer> docs = tkey.docs();
				SearchRequest request = tkey.request();
				ISearchable searcher = tkey.searcher();

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
				result.put("info", rsession.ghostBy("/menus/searchers").property("browsing").asString()) ;

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
					
					
					for (int did : docs) {
						ReadDocument rdoc = searcher.doc(did, request);
						JsonArray rowArray = new JsonArray() ;
						for(String fname : fnames){
							rowArray.add(new JsonPrimitive(rdoc.asString(fname, ""))) ;
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
	
	
	
	private Fqn fqnBy(String sid) {
		return Fqn.fromString("/searchers/" + IdString.create(sid).idString());
	}
}
