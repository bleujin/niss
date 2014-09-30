package net.ion.niss.webapp.searchers;

import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.Source;

import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.common.CSVStreamOut;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.JsonStreamOut;
import net.ion.niss.webapp.common.SourceStreamOut;
import net.ion.niss.webapp.indexers.Responses;
import net.ion.niss.webapp.indexers.SearchManager;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.radon.core.ContextParam;

import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

@Path("")
public class OpenSearchWeb {
	private ReadSession rsession;
	private SearchManager smanager;

	public OpenSearchWeb(@ContextParam("rentry") REntry rentry) throws IOException {
		this.rsession = rentry.login();
		this.smanager = rentry.searchManager();
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

			String template = rsession.pathBy(fqnBy(sid)).property(Def.Searcher.Template).asString();

			Engine engine = rsession.workspace().parseEngine();
			return engine.transform(template, MapUtil.<String, Object> chainMap().put("response", sresponse).put("params", map).toMap());
		} catch (net.ion.framework.mte.message.ParseException tex) {
			tex.printStackTrace();
			return tex.getMessage();
		}
	}

	private Fqn fqnBy(String sid) {
		return Fqn.fromString("/searchers/" + IdString.create(sid).idString());
	}
}
