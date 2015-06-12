package net.ion.niss.webapp.loaders;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Iterator;

import javax.script.ScriptException;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.GsonBuilder;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.DateUtil;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.niss.webapp.common.Trans;
import net.ion.niss.webapp.util.WebUtil;
import net.ion.radon.core.ContextParam;

import org.jboss.resteasy.spi.HttpResponse;

import com.google.common.base.Function;

@Path("/loaders")
public class LoaderWeb implements Webapp {

	private ReadSession rsession;
	private JScriptEngine jengine;
	private EventSourceEntry esentry;
	private REntry rentry;

	public LoaderWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine, @ContextParam("esentry") EventSourceEntry esentry) throws IOException {
		this.rentry = rentry ;
		this.rsession = rentry.login();
		this.jengine = jengine ;
		this.esentry = esentry ;
	}

	@GET
	@Path("")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject listScript() {
		JsonArray scripts = rsession.ghostBy("/loaders").children().ascending(Def.Loader.Created).transform(new Function<Iterator<ReadNode>, JsonArray>() {
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray();
				while (iter.hasNext()) {
					ReadNode node = iter.next();
					result.add(new JsonObject().put("lid", node.fqn().name()).put("name", node.property("name").asString()));
				}
				return result;
			}

		});
		
		JsonObject result = new JsonObject() ;
		result.add("loaders", scripts);
		result.put("info", rsession.ghostBy("/menus/loaders").property("overview").asString()) ;
		
		return result;

	}


	@POST
	@Path("/{lid}")
	public String create(@PathParam("lid") final String lid, @FormParam("name") final String name) throws Exception {
		return rsession.tranSync(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				if (wsession.exists("/loaders/" + lid)) return "already exist : " + lid ;
				wsession.pathBy("/loaders/" + lid).property("name", name).property(Def.Loader.Created, System.currentTimeMillis());;
				return "created " + lid;
			}
		});
	}
	
	@DELETE
	@Path("/{lid}")
	public String removeLoader(@PathParam("lid") final String lid) {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode found = wsession.pathBy("/loaders/" + lid);
				JsonObject decent = found.toReadNode().transformer(Trans.DECENT) ;
				StringBuilder sb = new StringBuilder();
				new GsonBuilder().setPrettyPrinting().create().toJson(decent, sb) ;
				
				FileUtil.forceWriteUTF8(new File(Webapp.REMOVED_DIR, "loader." + lid + ".bak"), sb.toString()) ;
				found.removeSelf() ;
				return null;
			}
		});
		return "deleted " + lid;
	}

	
	
	@POST
	@Path("/{lid}/define")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String defineLoader(@PathParam("lid") final String lid, @FormParam("content") final String content) {
		rsession.tran(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				WriteNode found = wsession.pathBy("/loaders/" + lid);
				FileUtil.forceWriteUTF8(new File(Webapp.REMOVED_DIR,  lid + ".loader.script.bak"), found.property(Def.Loader.Content).asString());
				
				found.property(Def.Loader.Content, content).property(Def.Loader.Created, System.currentTimeMillis());
				return lid;
			}
		});
		return "defined loader : " + lid;
	}



	@GET
	@Path("/{lid}/overview")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject overview(@PathParam("lid") final String lid) {
		
		JsonArray jarray = rsession.ghostBy("/events/loaders").children().eq("lid", lid).descending("time").offset(101).transform(new Function<Iterator<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray his = new JsonArray() ;
				while(iter.hasNext()){
					ReadNode node = iter.next() ;
					JsonArray row = new JsonArray() ;
					row.add(new JsonPrimitive(node.fqn().name()))
						.add(new JsonPrimitive(DateUtil.timeMilliesToDay(node.property(Def.Loader.Time).asLong(0)))  )
						.add(new JsonPrimitive(node.property(Def.Loader.Status).asString())) ;
					his.add(row) ;
				}
				return his;
			}
		}) ;
		
		JsonObject result = new JsonObject() ;
		result.add("history", jarray); 
		result.put("schemaName", JsonParser.fromString("[{'title':'EventId'},{'title':'Run Time'},{'title':'Status'}]").getAsJsonArray()) ;		
		result.put("info", rsession.ghostBy("/menus/loaders").property("overview").asString()) ;
		
		return result ;
	}


	
	@GET
	@Path("/{lid}/define")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewDefine(@PathParam("lid") final String lid) {
		ReadNode found = rsession.pathBy("/loaders/" + lid) ;
		
		JsonObject result = new JsonObject() ;
		result.put("info", rsession.ghostBy("/menus/loaders").property("define").asString()) ;
		result.put("lid", found.fqn().name()) ;
		result.put("samples", WebUtil.findLoaderScripts()) ;
		result.put(Def.Loader.Content, found.property(Def.Loader.Content).asString()) ;
		return result ;
	}


	@GET
	@Path("/{lid}/sample/{fileName}")
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String viewSampleScript(@PathParam("lid") final String lid, @PathParam("fileName") String fileName) throws IOException{
		return WebUtil.viewLoaderScript(fileName) ;
	}
	
	
	
	
	@POST
	@Path("/{lid}/run/{eventId}")
	public String run(@PathParam("lid") final String lid, @PathParam("eventId") final String eventId, @FormParam("content") final String content, @Context HttpResponse response) throws IOException, ScriptException{
		InstantJavaScript script = jengine.createScript(IdString.create(lid), "run at " + System.currentTimeMillis(), new StringReader(content)) ;
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/events/loaders/" +eventId)
					.property("lid", lid)
					.property("script", content)
					.property("status", "started").property("time", System.currentTimeMillis()) ;
				return null;
			}
		}) ;
		
		
		final Writer writer = esentry.newWriter(rentry, lid, eventId) ;
		
		script.execAsync(new ResultHandler<Void>() {
			@Override
			public Void onSuccess(Object result, Object... args) {
				IOUtil.close(writer);
				return null;
			}

			@Override
			public Void onFail(final Exception ex, Object... args) {
				rsession.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/events/loaders/" +eventId).property("status", "fail").property("exception", ex.getMessage()) ;
						return null;
					}
				}) ;
				
				try {
					writer.write(ex.getMessage()) ;
				} catch (IOException ignore) {}
				
				IOUtil.close(writer);
				return null;
			}
		}, writer);
		
		return eventId ;

	}

	
}
