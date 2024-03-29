package net.ion.niss.webapp.searchers;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
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

import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.radon.core.ContextParam;

@Path("/search")
public class OpenSearchWeb implements Webapp{
	private SearcherWeb referWeb;

	public OpenSearchWeb(@ContextParam(REntry.EntryName) REntry rentry, @ContextParam(QueryTemplateEngine.EntryName) QueryTemplateEngine qengine, @ContextParam(PopularQueryEntry.EntryName) PopularQueryEntry pqentry) throws IOException {
		this.referWeb = new SearcherWeb(rentry, qengine, pqentry) ;
	}

	// popular query
	
	@GET
	@Path("/{sid}/popularquery")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewPopularQuery(@PathParam("sid") final String sid) throws ExecutionException{
		return referWeb.viewPopularQuery(sid) ;
	}
	
	
	// --- query
	@GET
	@Path("/{sid}/query")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject query(@PathParam("sid") String sid) throws IOException {
		return referWeb.query(sid) ;
	}

	@GET
	@Path("/{sid}/query.json")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public StreamingOutput jquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		return referWeb.jquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@GET
	@Path("/{sid}/query.xml")
	@Produces(ExtMediaType.APPLICATION_XML_UTF8)
	public StreamingOutput xquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		return referWeb.xquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@GET
	@Path("/{sid}/query.csv")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public StreamingOutput cquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		return referWeb.cquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@GET
	@Path("/{sid}/query.template")
	public Response tquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @QueryParam("ishtml") boolean ishtml, @Context HttpRequest request) throws IOException, ParseException {

		return referWeb.tquery(sid, query, sort, skip, offset, indent, debug, ishtml, request) ;
	}
	

	// -- post
	
	@POST
	@Path("/{sid}/query.json")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public StreamingOutput jqueryPost(@PathParam("sid") String sid, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> queryParam = request.getUri().getQueryParameters() ;
		MultivaluedMap<String, String> formParam = request.getDecodedFormParameters() ;
		
		String query = StringUtil.coalesce(queryParam.getFirst("query"), formParam.getFirst("query"), "") ;
		String sort = StringUtil.coalesce(queryParam.getFirst("sort"), formParam.getFirst("sort"), "") ;
		String skip = StringUtil.coalesce(queryParam.getFirst("skip"), formParam.getFirst("skip"), "0") ;
		String offset = StringUtil.coalesce(queryParam.getFirst("offset"), formParam.getFirst("offset"), "10") ;
		boolean indent = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("indent"), formParam.getFirst("indent"), "false")) ;
		boolean debug = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("debug"), formParam.getFirst("debug"), "false")) ;
		
		return referWeb.jquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@POST
	@Path("/{sid}/query.xml")
	@Produces(ExtMediaType.APPLICATION_XML_UTF8)
	public StreamingOutput xqueryPost(@PathParam("sid") String sid, @Context HttpRequest request) throws IOException, ParseException {
		MultivaluedMap<String, String> queryParam = request.getUri().getQueryParameters() ;
		MultivaluedMap<String, String> formParam = request.getDecodedFormParameters() ;
		
		String query = StringUtil.coalesce(queryParam.getFirst("query"), formParam.getFirst("query"), "") ;
		String sort = StringUtil.coalesce(queryParam.getFirst("sort"), formParam.getFirst("sort"), "") ;
		String skip = StringUtil.coalesce(queryParam.getFirst("skip"), formParam.getFirst("skip"), "0") ;
		String offset = StringUtil.coalesce(queryParam.getFirst("offset"), formParam.getFirst("offset"), "10") ;
		boolean indent = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("indent"), formParam.getFirst("indent"), "false")) ;
		boolean debug = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("debug"), formParam.getFirst("debug"), "false")) ;
		

		return referWeb.xquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@POST
	@Path("/{sid}/query.csv")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public StreamingOutput cqueryPost(@PathParam("sid") String sid, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> queryParam = request.getUri().getQueryParameters() ;
		MultivaluedMap<String, String> formParam = request.getDecodedFormParameters() ;
		
		String query = StringUtil.coalesce(queryParam.getFirst("query"), formParam.getFirst("query"), "") ;
		String sort = StringUtil.coalesce(queryParam.getFirst("sort"), formParam.getFirst("sort"), "") ;
		String skip = StringUtil.coalesce(queryParam.getFirst("skip"), formParam.getFirst("skip"), "0") ;
		String offset = StringUtil.coalesce(queryParam.getFirst("offset"), formParam.getFirst("offset"), "10") ;
		boolean indent = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("indent"), formParam.getFirst("indent"), "false")) ;
		boolean debug = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("debug"), formParam.getFirst("debug"), "false")) ;
		
		return referWeb.cquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@POST
	@Path("/{sid}/query.template")
	public Response tqueryPost(@PathParam("sid") String sid, @Context HttpRequest request) throws IOException, ParseException {

		MultivaluedMap<String, String> queryParam = request.getUri().getQueryParameters() ;
		MultivaluedMap<String, String> formParam = request.getDecodedFormParameters() ;
		
		String query = StringUtil.coalesce(queryParam.getFirst("query"), formParam.getFirst("query"), "") ;
		String sort = StringUtil.coalesce(queryParam.getFirst("sort"), formParam.getFirst("sort"), "") ;
		String skip = StringUtil.coalesce(queryParam.getFirst("skip"), formParam.getFirst("skip"), "0") ;
		String offset = StringUtil.coalesce(queryParam.getFirst("offset"), formParam.getFirst("offset"), "10") ;
		boolean indent = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("indent"), formParam.getFirst("indent"), "false")) ;
		boolean debug = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("debug"), formParam.getFirst("debug"), "false")) ;
		boolean ishtml = Boolean.getBoolean(StringUtil.coalesce(queryParam.getFirst("ishtml"), formParam.getFirst("ishtml"), "false")) ;
		
		return referWeb.tquery(sid, query, sort, skip, offset, indent, debug, ishtml, request) ;
	}
}
