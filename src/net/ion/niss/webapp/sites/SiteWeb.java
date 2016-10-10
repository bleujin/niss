package net.ion.niss.webapp.sites;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
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
import javax.ws.rs.core.Response;

import net.coobird.thumbnailator.Thumbnailator;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.builders.ThumbnailParameterBuilder;
import net.coobird.thumbnailator.tasks.StreamThumbnailTask;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.DateUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.niss.config.NSConfig;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.IndexSchema;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.niss.webapp.common.MyAuthenticationHandler;
import net.ion.niss.webapp.indexers.SchemaInfos;
import net.ion.radon.core.ContextParam;

import org.apache.lucene.queryparser.classic.ParseException;
import org.jboss.resteasy.spi.HttpRequest;

import com.google.common.base.Function;


@Path("/sites")
public class SiteWeb implements Webapp {
	

	private ReadSession rsession;
	private SiteManager smanager;
	private IDBController dc;
	private NSConfig nsConfig;

	public SiteWeb(@ContextParam("rentry") REntry rentry, @ContextParam("IDBController") IDBController dc) throws IOException {
		this.rsession = rentry.login();
		this.smanager = rentry.siteManager() ;
		this.dc = dc ;
		this.nsConfig = rentry.nsConfig() ;
	}

	@GET
	@Path("")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonArray listSite() {
		ReadChildren children = rsession.ghostBy("/sites").children().ascending(Def.Site.Created);

		return children.transform(new Function<Iterator<ReadNode>, JsonArray>() {
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray();
				while (iter.hasNext()) {
					ReadNode node = iter.next();
					String name = node.fqn().name();
					result.add(new JsonObject().put("sid", name).put("name", name));
				}
				return result;
			}
		});
	}

	// create site
	@POST
	@Path("/{sid}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String createSite(@PathParam("sid") final String sid, @FormParam("siteurl") final String siteUrl) throws Exception {

		return rsession.tranSync(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				if (wsession.readSession().exists(fqnBy(sid))) return "already exist : " + sid ;
				wsession.pathBy(fqnBy(sid)).property(Def.Site.SiteUrl, siteUrl).property(Def.Site.Created, System.currentTimeMillis());
				return "created " + sid;
			}
		});
	}
	
	
	// remove 
	@DELETE
	@Path("/{sid}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String removeSite(@PathParam("sid") final String sid){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode found = wsession.pathBy(fqnBy(sid));
				Set<String> names = found.childrenNames() ;
				for (String crawlId : names) {
					dc.createUserProcedure("Crawl@removeWith(?)").addParam(crawlId).execUpdate() ;
				}
				
				found.removeSelf() ;
				return null;
			}
		}) ;
		
		return "removed " + sid;
	}

	
	@GET
	@Path("/{sid}/overview")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject overview(@PathParam("sid") final String sid) throws IOException{
		
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/sites").property("overview").asString()) ;
		
		ReadNode snode = rsession.pathBy("/sites/" + sid) ;
		
		JsonObject status = new JsonObject() ;
		status.put("siteUrl", snode.property(Def.Site.SiteUrl).asString()) ;
		JsonArray clist = new JsonArray() ;
		for(ReadNode cnode : snode.children().descending("created").offset(5).iterator()){
			String json = new JsonObject().put("created", DateUtil.longToCalendar(cnode.property("created").asLong(0))).put("creUserId", cnode.property("creuserid").asString()).toString() ;
			clist.add(new JsonPrimitive(json)) ;
		}
		status.put("recent crawl", clist) ;
		result.add("status", status);
		
		return result;
	}
	

	@GET
	@Path("/{sid}/crawl")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject crawlView(@PathParam("sid") final String sid) throws IOException{
		
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/sites").property("crawl").asString()) ;
		
		return result;
	}
	

	@POST
	@Path("/{sid}/crawl")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String crawlSite(@PathParam("sid") final String sid, @FormParam("coption") final String jsonotpion, @Context final HttpRequest request) throws Exception{

		ReadNode snode = rsession.pathBy("/sites/" + sid) ;
		final CrawlOption coption = CrawlOption.loadFrom(jsonotpion) ;
		coption.siteUrl(snode.property(Def.Site.SiteUrl).asString()) ;
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				String userName = ObjectUtil.coalesce(request.getAttribute(MyAuthenticationHandler.USERNAME), "anonymous").toString();
				wsession.pathBy(fqnBy(sid)).child(coption.crawlId()).property("created", new Date().getTime()).property("creuserid", userName) ;
				smanager.crawlSite(dc, coption);
				return null;
			}
		}) ;
		
		return coption.crawlId() ;
	}
	
	
	
	
	@POST
	@Path("/{sid}/index")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String indexSite(@PathParam("sid") final String sid, @FormParam("crawlid") final String crawlId, @FormParam("iid") final String iid, @Context final HttpRequest request) throws Exception{
		
		final SchemaInfos sinfos = rsession.ghostBy(IndexSchema.path(iid)).children().transform(new Function<Iterator<ReadNode>, SchemaInfos>(){
			@Override
			public SchemaInfos apply(Iterator<ReadNode> iter) {
				return SchemaInfos.create(iter) ;
			}
		}) ;
		
		smanager.indexCrawlSite(dc, sinfos, iid, crawlId); 
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/sites/" + sid, crawlId).property("indexed", true).property("iid", iid) ;
				return null;
			}
		}) ;
		return crawlId ;
	}

	
	@POST
	@Path("/{sid}/capture")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String captureSite(@PathParam("sid") final String sid, @FormParam("crawlid") final String crawlId, @Context final HttpRequest request) throws Exception{
		
		
		smanager.makeCapture(dc, crawlId); 
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/sites/" + sid, crawlId).property("captured", true) ;
				return null;
			}
		}) ;
		return crawlId ;
	}

	@GET
	@Path("/{sid}/capture_image/{remain: [A-Za-z0-9_\\.\\/]*}")
	@Produces("image/png")
	public File viewCapture(@PathParam("remain") String remain){
		return new File(nsConfig.siteConfig().screenHomeDir(), remain) ;
	}
	
	@GET
	@Path("/{sid}/capture_thumbnail/{remain: [A-Za-z0-9_\\.\\/]*}")
	@Produces("image/png")
	public Response viewCaptureThumbnail(@PathParam("remain") String remain, @QueryParam("width") @DefaultValue("100") final int width, @QueryParam("height") @DefaultValue("100") final int height) throws IOException{
		
		File oriFile = viewCapture(remain) ;
		if (! oriFile.exists()) return Response.status(404).entity("not found file").build() ;
		
		File makedFile = viewCapture(remain + ".thumb" + width + "x" + height + ".png") ;
		if (makedFile.exists()) return Response.ok().entity(makedFile).type(ExtMediaType.valueOf("image/png")).build() ;
		
        Thumbnails.of(oriFile).forceSize(width, height).outputFormat("png").toFile(makedFile);
        return Response.ok().entity(makedFile).type(ExtMediaType.valueOf("image/png")).build() ;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	// --- page
	
	

	@GET
	@Path("/{sid}/crawllist")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject browsingCrawl(@PathParam("sid") String sid, @QueryParam("searchQuery") String query, @Context HttpRequest request) throws IOException, ParseException{
		final JsonObject result = new JsonObject() ;

		result.put("info", rsession.ghostBy("/menus/sites").property("browsing").asString()) ;

		JsonArray schemaNames = new JsonArray();
		schemaNames.add(new JsonObject().put("title", "crawlId")) ;
		schemaNames.add(new JsonObject().put("title", "created")) ;
		schemaNames.add(new JsonObject().put("title", "creUserId")) ;
		schemaNames.add(new JsonObject().put("title", "indexed")) ;
		schemaNames.add(new JsonObject().put("title", "captured")) ;
		result.put("schemaName", schemaNames) ;
		
		
		JsonArray dataArray = new JsonArray() ;
		for (ReadNode cnode : rsession.ghostBy("/sites/" + sid).children().descending("created").iterator()) {
			
			JsonArray rowArray = new JsonArray() ;
			rowArray.add(new JsonPrimitive(cnode.fqn().name())) ;
			rowArray.add(new JsonPrimitive(cnode.property("created").asLong(0))) ;
			rowArray.add(new JsonPrimitive(cnode.property("creuserid").asString())) ;
			rowArray.add(new JsonPrimitive(cnode.property("indexed").asBoolean())) ;
			rowArray.add(new JsonPrimitive(cnode.property("captured").asBoolean())) ;
			dataArray.add(rowArray);
		}
		result.put("data", dataArray) ;

		return result;
	}
	
	@POST
	@Path("/{sid}/crawllist")
	public String removeCrawl(@PathParam("sid") final String sid, @DefaultValue("") @FormParam("crawlid") final String crawlid, @FormParam("removeindex") boolean removeindex){
		
		Boolean indexed = rsession.ghostBy(fqnBy(sid, crawlid)).property("indexed").asBoolean() ;
		String iid = rsession.ghostBy(fqnBy(sid, crawlid)).property("iid").asString() ;
		
		if (removeindex && indexed && smanager.hasIndex(iid)){
			smanager.indexRemove(iid, crawlid) ;
		}
		
		rsession.tran(new TransactionJob<Integer>() {
			@Override
			public Integer handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqnBy(sid)).child(crawlid).removeSelf() ;
				dc.createUserProcedure("crawl@removeWith(?)").addParam(crawlid).execUpdate() ;
				return 1;
			}
		}) ;
		
		return " removed" ;
	}
	
	@GET
	@Path("/{sid}/{cid}/pagelist")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject browsingPage(@PathParam("sid") String sid, @PathParam("cid") String cid, @QueryParam("searchQuery") String query, @Context HttpRequest request) throws IOException, ParseException, SQLException{
		final JsonObject result = new JsonObject() ;

		JsonArray schemaNames = new JsonArray();
		schemaNames.add(new JsonObject().put("title", "url")) ;
		schemaNames.add(new JsonObject().put("title", "scode")) ;
		schemaNames.add(new JsonObject().put("title", "screenpath")) ;
		schemaNames.add(new JsonObject().put("title", "urlhash")) ;
		result.put("schemaName", schemaNames) ;
		
		final JsonArray dataArray = new JsonArray() ;
		dc.createUserProcedure("Crawl@pageListBy(?)").addParam(cid).execHandlerQuery(new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				while(rs.next()){
					JsonArray rowArray = new JsonArray() ;
					rowArray.add(JsonPrimitive.createDefault(rs.getString("url"), "")) ;
					rowArray.add(JsonPrimitive.createDefault(rs.getString("scode"), "")) ;
					rowArray.add(JsonPrimitive.createDefault(rs.getString("screenpath"), "")) ;
					rowArray.add(JsonPrimitive.createDefault(rs.getString("urlhash"), "")) ;
					dataArray.add(rowArray);
				}
				return null;
			}
		}) ;
		result.put("data", dataArray) ;

		return result;
	}
	
	
	@GET
	@Path("/{sid}/{cid}/{urlhash}/linklist")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject browsingRefer(@PathParam("sid") String sid, @PathParam("cid") String cid, @PathParam("urlhash") String urlhash, @QueryParam("searchQuery") String query, @Context HttpRequest request) throws IOException, ParseException, SQLException{
		final JsonObject result = new JsonObject() ;

		JsonArray schemaNames = new JsonArray();
		schemaNames.add(new JsonObject().put("title", "typecd")) ;
		schemaNames.add(new JsonObject().put("title", "url")) ;
		schemaNames.add(new JsonObject().put("title", "anchor")) ;
		result.put("schemaName", schemaNames) ;
		
		final JsonArray dataArray = new JsonArray() ;
		dc.createUserProcedure("Crawl@linkListBy(?,?)").addParam(cid).addParam(urlhash).execHandlerQuery(new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				while(rs.next()){
					JsonArray rowArray = new JsonArray() ;
					rowArray.add(JsonPrimitive.createDefault(rs.getString("typecd"), "")) ;
					rowArray.add(JsonPrimitive.createDefault(rs.getString("url"), "")) ;
					rowArray.add(JsonPrimitive.createDefault(rs.getString("anchor"), "")) ;
					dataArray.add(rowArray);
				}
				return null;
			}
		}) ;
		result.put("data", dataArray) ;

		return result;
	}
	
	
	
	
	
	@GET
	@Path("/{sid}/hello")
	public String hello(@PathParam("sid") String sid){
		return sid ;
	}
	
	
	
	private Fqn fqnBy(String sid) {
		return Fqn.fromString("/sites/" + IdString.create(sid).idString());
	}

	private Fqn fqnBy(String sid, String crawlId) {
		return Fqn.fromString("/sites/" + IdString.create(sid).idString() + "/" + crawlId);
	}

}
