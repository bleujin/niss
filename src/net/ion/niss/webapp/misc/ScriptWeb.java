package net.ion.niss.webapp.misc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;
import net.ion.niss.webapp.util.WebUtil;
import net.ion.radon.core.ContextParam;

import org.apache.commons.collections.map.MultiValueMap;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;

import com.google.common.base.Function;

@Path("/scripts")
public class ScriptWeb implements Webapp{

	private ReadSession rsession;
	private JScriptEngine jengine;
	private REntry rentry;
	public ScriptWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine ) throws IOException{
		this.rentry = rentry ;
		this.rsession = rentry.login() ;
		this.jengine = jengine ;
	}
	
	
	
	@Path("/define/{sid}")
	@POST
	public String defineScript(@PathParam("sid") final String sid, @DefaultValue("") @FormParam("content") final String content){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/scripts/" + sid).property("content", content) ;
				return null;
			}
		}) ;
		return sid + " created" ;
	}
	
	@Path("/define/{sid}")
	@GET
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject viewScript(@PathParam("sid") final String sid){
		ReadNode found = rsession.ghostBy("/scripts/" + sid) ;
		return new JsonObject()
			.put("sid", found.fqn().name())
			.put("samples", WebUtil.findScripts())
			.put("content", found.property("content").asString()) ;
	}

	@Path("/sample/{fileName}")
	@GET
	@Produces(ExtMediaType.TEXT_PLAIN_UTF8)
	public String sampleScript(@PathParam("fileName") String fileName) throws IOException{
		return WebUtil.viewScript(fileName) ;
	}
	


	@Path("/remove/{sid}")
	@DELETE
	public String removeScript(@PathParam("sid") final String sid){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/scripts/" + sid).removeSelf() ;
				return null;
			}
		}) ;
		return sid + " removed" ;
	}
	
	@POST
	@Path("/removes")
	public String removeUsers(@FormParam("scripts") final String scripts){
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				String[] targets = StringUtil.split(scripts, ",") ;
				for (String userId : targets) {
					wsession.pathBy("/scripts/" + userId).removeSelf() ;
				}
				return null ;
			}
		});
		return "removed " + scripts ; 
	}
	
	
	@Path("")
	@GET
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public JsonObject listScript(){
		JsonArray jarray = rsession.ghostBy("/scripts").children().transform(new Function<Iterator<ReadNode>, JsonArray>(){
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray() ;
				try {
					while (iter.hasNext()) {
						ReadNode node = iter.next();
						JsonArray userProp = new JsonArray();
						userProp.add(new JsonPrimitive(node.fqn().name()));
						userProp.add(new JsonPrimitive("/open/script/run/" + node.fqn().name()));
						String firstLine = new BufferedReader(new StringReader(node.property("content").asString())).readLine();
						userProp.add(new JsonPrimitive(StringUtil.defaultString(firstLine, "")));
						result.add(userProp);
					}
				} catch (IOException ignore) {
				}

				return result;
			}
		}) ;
		return new JsonObject()
				.put("info", rsession.ghostBy("/menus/misc").property("script").asString())
				.put("schemaName", JsonParser.fromString("[{'title':'Id'},{'title':'Run Path'},{'title':'Explain'}]").getAsJsonArray())
				.put("samples", WebUtil.findScripts())
				.put("scripts", jarray) ;
	}
	

	@Path("/instantrun")
	@POST
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public Response instantRunScript(@Context HttpRequest request, @DefaultValue("") @FormParam("content") String content) throws IOException, ScriptException{
		String scriptId = "" + System.currentTimeMillis() ;
		return runScript(scriptId, new MultivaluedMapImpl<String, String>(), content) ;
	}
	
	@Path("/run/{sid}")
	@GET @POST
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public Response runScript(@PathParam("sid") String sid, @Context HttpRequest request) throws IOException, ScriptException{
		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		for (Entry<String, List<String>> entry : request.getUri().getQueryParameters().entrySet()) {
			if (StringUtil.isNotBlank(entry.getKey())) params.put(entry.getKey(), entry.getValue()) ;
		}
		
		for (Entry<String, List<String>> entry : request.getDecodedFormParameters().entrySet()) {
			if (StringUtil.isNotBlank(entry.getKey())) params.put(entry.getKey(), entry.getValue()) ;
		}

		String content = rsession.ghostBy("/scripts/" + sid).property("content").asString() ;
		return runScript(sid, params, content);
	}



	private Response runScript(String scriptId, MultivaluedMap<String, String> params, String content) throws IOException, ScriptException {
		final StringWriter writer = new StringWriter();
		InstantJavaScript script = jengine.createScript(IdString.create(scriptId), "", new StringReader(content)) ;
		
		StringWriter result = new StringWriter();
		final JsonWriter jwriter =  new JsonWriter(result) ;
		jengine.execHandle(script, new ResultHandler<Void>() {
			@Override
			public Void onSuccess(Object result, Object... args) {
				try {
					jwriter.beginObject().name("return").value(ObjectUtil.toString(result)) ;
				} catch (IOException ignore) {
				}
				return null;
			}

			@Override
			public Void onFail(Exception ex, Object... args) {
				try {
					jwriter.beginObject().name("return").value("").name("exception").value(ex.getMessage()) ;
				} catch (IOException e) {
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

		return Response.ok(result.toString(), ExtMediaType.APPLICATION_JSON_UTF8).build() ;
	}

}
