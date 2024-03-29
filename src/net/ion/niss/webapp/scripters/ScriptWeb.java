package net.ion.niss.webapp.scripters;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.script.ScriptException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.WriteJob;
import net.bleujin.rcraken.WriteNode;
import net.bleujin.rcraken.WriteSession;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.DateUtil;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.SLog;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;
import net.ion.niss.webapp.misc.HttpParams;
import net.ion.niss.webapp.util.WebUtil;
import net.ion.radon.core.ContextParam;

@Path("/scripters")
public class ScriptWeb implements Webapp{

	private ReadSession rsession;
	private JScriptEngine jengine;
	private REntry rentry;
	private EventSourceEntry esentry;
	
	public ScriptWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine, @ContextParam("esentry") EventSourceEntry esentry ) throws IOException{
		this.rentry = rentry ;
		this.rsession = rentry.login() ;
		this.jengine = jengine ;
		this.esentry = esentry ;
	}
	
	
	@Path("")
	@GET
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject listScript(){
		JsonArray jarray = rsession.pathBy("/scripts").children().stream().transform(new Function<Iterable<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterable<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				try {
					for (ReadNode node : iter) {
						JsonArray userProp = new JsonArray();
						userProp.add(new JsonPrimitive(node.fqn().name()));
//						userProp.add(new JsonPrimitive("/open/script/run/" + node.fqn().name()));
						String firstLine = new BufferedReader(new StringReader(node.property(Def.Script.Content).asString())).readLine();
						userProp.add(new JsonPrimitive(StringUtil.defaultString(firstLine, "")));
						result.add(userProp);
					}
				} catch (IOException ignore) {
				}

				return result;
			}
		}) ;
		return new JsonObject()
//				.put("info", rsession.pathBy("/menus/misc").property("script").asString())
				.put("schemaName", JsonParser.fromString("[{'title':'Id'},{'title':'Run Path'},{'title':'Explain'}]").getAsJsonArray())
//				.put("samples", WebUtil.findScripts())
				.put("scripters", jarray) ;
	}
	

	@Path("/{sid}/overview")
	@GET
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject overview(@PathParam(Def.Script.Sid) final String sid){
		
		JsonArray slogs = rsession.pathBy("/scripts/" + sid + "/slogs").children().stream().descending(SLog.Runtime).transform(new Function<Iterable<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterable<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				for (ReadNode node : iter) {
					JsonArray userProp = new JsonArray() ;
					userProp.add(new JsonPrimitive(node.fqn().name())) ;
					userProp.add(new JsonPrimitive(DateUtil.timeMilliesToDay(node.property(SLog.Runtime).asLong()))) ;
					userProp.add(new JsonPrimitive(node.property(SLog.Status).asString())) ;
					userProp.add(new JsonPrimitive(node.property(SLog.Result).asString())) ;
					result.add(userProp) ;
				}

				return result;
			}
		}) ;

		JsonObject result = new JsonObject() ;
		result.add("slogs", slogs); 
		result.put("schemaName", JsonParser.fromString("[{'title':'Id'},{'title':'Run Time'},{'title':'Status'}]").getAsJsonArray()) ;		
		result.put("info", rsession.pathBy("/scripters/" + sid + "/info").property("overview").asString()) ;
		
		return result ;
	}
	

	@Path("/{sid}")
	@DELETE
	public String removeScript(@PathParam(Def.Script.Sid) final String sid){
		rsession.tran(wsession -> {
			WriteNode found = wsession.pathBy("/scripts/" + sid);
			FileUtil.forceWriteUTF8(new File(Webapp.REMOVED_DIR,  sid + ".misc.script.bak"), found.property(Def.Script.Content).asString());
			found.removeSelf() ;
		}) ;
		return sid + " removed" ;
	}
	
	
	@Path("/{sid}/define")
	@GET
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewScript(@PathParam(Def.Script.Sid) final String sid){
		ReadNode found = rsession.pathBy("/scripts/" + sid) ;
		return new JsonObject()
			.put("sid", found.fqn().name())
			.put("samples", WebUtil.findScripts())
			.put("info", rsession.pathBy("/scripters/" + sid + "/info").property("define").asString()) 
			.put("content", found.property("content").asString()) ;
	}
	

	
	@Path("/{sid}/define")
	@POST
	public String defineScript(@PathParam(Def.Script.Sid) final String sid, @DefaultValue("") @FormParam(Def.Script.Content) final String content){
		rsession.tran(wsession -> {
			WriteNode found = wsession.pathBy("/scripts/" + sid);
			FileUtil.forceWriteUTF8(new File(Webapp.REMOVED_DIR,  sid + ".misc.script.bak"), found.property(Def.Script.Content).asString());
			
			found.property(Def.Script.Content, content).merge();
		}) ;
		return sid + " created" ;
	}


	@Path("/{sid}/samplescript/{fileName}")
	@GET
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String sampleScript(@PathParam("fileName") String fileName) throws IOException{
		return WebUtil.viewScript(fileName) ;
	}
	

	
	@POST
	@Path("/{sid}/removes")
	public String removeScripts(@FormParam("scripts") final String scripts){
		rsession.tran( wsession -> {
			String[] targets = StringUtil.split(scripts, ",") ;
			for (String userId : targets) {
				wsession.pathBy("/scripts/" + userId).removeSelf() ;
			}
		});
		return "removed " + scripts ; 
	}
	

	@Path("/{sid}/run")
	@POST
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public Response runScript(@PathParam(Def.Script.Sid) String sid, @Context HttpRequest request) throws IOException, ScriptException{
		
		HttpParams params = HttpParams.create(request) ;
		
		String content = rsession.pathBy("/scripts/" + sid).property(Def.Script.Content).asString() ;
		StringWriter writer = new StringWriter();
		return runScript(sid, writer, params, content);
	}
	
	public Response runTestScript(@PathParam(Def.Script.Sid) String sid, MultivaluedMap<String, String> params, String content) throws IOException, ScriptException{
		StringWriter writer = new StringWriter();
		return runScript(sid, writer, params, content);
	}
	

	
	@Path("/{sid}/instantrun/{eventid}")
	@POST
	public Response instantRunScript(@PathParam("sid") final String sid, @PathParam("eventid") String eventId, @DefaultValue("") @FormParam("content") String content) throws IOException, ScriptException{

		final HttpParams params = new HttpParams() ;
		
		final CountDownLatch latch = esentry.createEvent(eventId) ;
		final ScriptOutWriter writer = new ScriptOutWriter(esentry, eventId, latch) ;

		InstantJavaScript script = jengine.createScript(IdString.create(sid), "", new StringReader(content)) ;
		script.execAsync(new ResultHandler<Void>() {
			@Override
			public Void onSuccess(Object result, Object... args) {
				try {
					writer.write("\ncomplete :\n");
					writer.write(ObjectUtil.toString(result));
					writer.flush(); 
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					IOUtil.closeQuietly(writer);
					rsession.tran(end(sid, "instant success", result)) ;
				}
				
				return null;
			}

			@Override
			public Void onFail(Exception ex, Object... args) {
				try {
					writer.write("\nexception occured : " + ex.getMessage() + "\n") ;
					writer.flush(); 
					ex.printStackTrace(); 
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					IOUtil.closeQuietly(writer);
					rsession.tran(end(sid, "instant fail", ex.getMessage())) ;
				}
				return null;
			}
		}, writer, rsession, params, rentry, jengine) ;

		
		return Response.ok().build() ;
	}

	
	public static WriteJob<Void> end(final String sid, final String status, final Object result){
		return new WriteJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode logNode = wsession.pathBy(SLog.path(sid));
				long cindex = logNode.property(SLog.CIndex).asLong() ;
				logNode.child("c" + cindex).property(SLog.Runtime, System.currentTimeMillis()).property(SLog.Status, status).property(SLog.Result, ObjectUtil.toString(result)).merge();
				logNode.property(SLog.CIndex, (++cindex) % 101).merge();
				return null;
			}
		} ;
	}
	
	
	


	private Response runScript(final String scriptId, Writer writer, MultivaluedMap<String, String> params, String content) throws IOException, ScriptException {
		InstantJavaScript script = jengine.createScript(IdString.create(scriptId), "", new StringReader(content)) ;
		
		StringWriter result = new StringWriter();
		final JsonWriter jwriter =  new JsonWriter(result) ;
		script.exec(new ResultHandler<Void>() {
			@Override
			public Void onSuccess(Object result, Object... args) {
				try {
					jwriter.beginObject().name("return").value(ObjectUtil.toString(result)) ;
				} catch (IOException ignore) {
				} finally{
					rsession.tran(end(scriptId, "run success", result)) ;
				}
				return null;
			}

			@Override
			public Void onFail(Exception ex, Object... args) {
				try {
					jwriter.beginObject().name("return").value("").name("exception").value(ex.getMessage()) ;
				} catch (IOException e) {
				} finally {
					rsession.tran(end(scriptId, "run fail", ex.getMessage())) ;
				}
				return null;
			}
		}, writer, rsession, params, rentry, jengine) ;
		
		jwriter.name("writer").value(writer.toString()) ;
		
		jwriter.name("params") ;
		jwriter.beginArray() ;
		for (Entry<String, List<String>> entry : params.entrySet()) {
			jwriter.beginObject().name(entry.getKey()).beginArray() ;
			for(String val : entry.getValue()){
				jwriter.value(val) ;
			}
			jwriter.endArray().endObject() ;
		}
		jwriter.endArray() ;
		jwriter.endObject() ;
		jwriter.close();

		
		// write log
		
		return Response.ok(result.toString(), ExtMediaType.APPLICATION_JSON_UTF8).build() ;
	}

	

	
	
	@Path("/{sid}/schedule")
	@GET
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewScheduleInfo(@PathParam(Def.Script.Sid) final String sid){
		ReadNode sinfo = rsession.pathBy("/scripts/" + sid + "/schedule") ;

		
		JsonObject sinfoJson = new JsonObject()
			.put(Def.Schedule.MINUTE, sinfo.property(Def.Schedule.MINUTE).defaultValue(""))
			.put(Def.Schedule.HOUR, sinfo.property(Def.Schedule.HOUR).defaultValue(""))
			.put(Def.Schedule.DAY, sinfo.property(Def.Schedule.DAY).defaultValue(""))
			.put(Def.Schedule.MONTH, sinfo.property(Def.Schedule.MONTH).defaultValue(""))
			.put(Def.Schedule.WEEK, sinfo.property(Def.Schedule.WEEK).defaultValue(""))
			.put(Def.Schedule.MATCHTIME, sinfo.property(Def.Schedule.MATCHTIME).defaultValue(""))
			.put(Def.Schedule.YEAR, sinfo.property(Def.Schedule.YEAR).defaultValue(""))
			.put(Def.Schedule.ENABLE, sinfo.property(Def.Schedule.ENABLE).defaultValue(false)) ;
		
		JsonArray slogs = rsession.pathBy("/scripts/" + sid + "/slogs").children().stream().transform(new Function<Iterable<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterable<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				for (ReadNode node : iter) {
					JsonArray userProp = new JsonArray() ;
					userProp.add(new JsonPrimitive(node.fqn().name())) ;
					userProp.add(new JsonPrimitive(node.property("runtime").asLong())) ;
					result.add(userProp) ;
				}

				return result;
			}
		}) ;
		return new JsonObject().put("info", rsession.pathBy("/scripters/" + sid + "/info").property("schedule").asString())
				.put("sinfo", sinfoJson)
				.put("schemaName", JsonParser.fromString("[{'title':'Id'},{'title':'Run Time'}]").getAsJsonArray())
				.put("slogs", slogs) ;
	}
	
	
	
	@Path("/{sid}/schedule")
	@POST
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String editScheduleInfo(@PathParam(Def.Script.Sid) final String sid, 
			@DefaultValue("0-59") @FormParam(Def.Schedule.MINUTE) final String minute, @DefaultValue("0-23") @FormParam(Def.Schedule.HOUR) final String hour, 
			@DefaultValue("1-31") @FormParam(Def.Schedule.DAY) final String day, @DefaultValue("1-12") @FormParam(Def.Schedule.MONTH) final String month, 
			@DefaultValue("1-7") @FormParam(Def.Schedule.WEEK) final String week, @DefaultValue("-1") @FormParam(Def.Schedule.MATCHTIME) final String matchtime, 
			@DefaultValue("2014-2020") @FormParam(Def.Schedule.YEAR) final String year, @DefaultValue("false") @FormParam(Def.Schedule.ENABLE) final boolean enable){
		
		
		rsession.tran(wsession -> {
			WriteNode found = wsession.pathBy("/scripts/" + sid + "/schedule");
			found.property(Def.Schedule.MINUTE, minute)
				.property(Def.Schedule.HOUR, hour)
				.property(Def.Schedule.DAY, day)
				.property(Def.Schedule.MONTH, month)
				.property(Def.Schedule.WEEK, week)
				.property(Def.Schedule.MATCHTIME, matchtime)
				.property(Def.Schedule.YEAR, year) 
				.property(Def.Schedule.ENABLE, enable).merge();
		}) ;
		
		return sid + (enable ? " rescheduled" : " canceled" );
	}
	
	
	
	
	
}



class ScriptOutWriter extends Writer{

	private String eventId;
	private EventSourceEntry ese;
	private StringBuilder buffer = new StringBuilder() ;
	private CountDownLatch latch;

	public ScriptOutWriter(EventSourceEntry ese, String eventId, CountDownLatch latch) throws IOException {
		this.ese = ese ;
		this.eventId = eventId ;
		this.latch = latch ;
 	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		buffer.append(cbuf, off, len) ;
	}
	
	public ScriptOutWriter writeLn(String msg) throws IOException{
		super.write(msg);
		this.flush(); 
		return this ;
	}

	@Override
	public void flush() throws IOException {
		try {
			latch.await(1, TimeUnit.SECONDS) ;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ese.sendTo(eventId, buffer.toString()) ;
		buffer.setLength(0);
	}

	@Override
	public void close() throws IOException {
		flush();
		ese.closeEvent(eventId); 
	}
}
