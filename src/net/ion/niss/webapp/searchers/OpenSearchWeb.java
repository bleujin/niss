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
import javax.ws.rs.core.StreamingOutput;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.niss.webapp.REntry;
import net.ion.radon.core.ContextParam;

import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

@Path("/search")
public class OpenSearchWeb {
	private SearcherWeb referWeb;

	public OpenSearchWeb(@ContextParam("rentry") REntry rentry, @ContextParam("qtemplate") QueryTemplateEngine qengine) throws IOException {
		this.referWeb = new SearcherWeb(rentry, qengine) ;
	}

	// --- query
	@GET
	@Path("/{sid}/query")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject query() throws IOException {
		return referWeb.query() ;
	}

	@GET
	@Path("/{sid}/query.json")
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput jquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		return referWeb.jquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@GET
	@Path("/{sid}/query.xml")
	@Produces(MediaType.APPLICATION_XML)
	public StreamingOutput xquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		return referWeb.xquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@GET
	@Path("/{sid}/query.csv")
	@Produces(MediaType.TEXT_PLAIN)
	public StreamingOutput cquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		return referWeb.cquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}

	@GET
	@Path("/{sid}/query.template")
	public String tquery(@PathParam("sid") String sid, @DefaultValue("") @QueryParam("query") String query, @DefaultValue("") @QueryParam("sort") String sort, @DefaultValue("0") @QueryParam("skip") String skip, @DefaultValue("10") @QueryParam("offset") String offset,
			@QueryParam("indent") boolean indent, @QueryParam("debug") boolean debug, @Context HttpRequest request) throws IOException, ParseException {

		return referWeb.tquery(sid, query, sort, skip, offset, indent, debug, request) ;
	}
}
