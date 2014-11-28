package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.script.ScriptException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import net.ion.craken.node.ReadSession;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;
import net.ion.radon.core.ContextParam;
import net.ion.radon.core.let.FileResponseBuilder;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;

@Path("/script")
public class OpenScriptWeb implements Webapp{

	private ScriptWeb refWeb;
	private REntry rentry;
	private ReadSession rsession;
	private JScriptEngine jengine;
	private EventSourceEntry esentry;

	public OpenScriptWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine, @ContextParam("esentry") EventSourceEntry esentry ) throws IOException{
		this.refWeb = new ScriptWeb(rentry, jengine) ;
		this.rentry = rentry ;
		this.rsession = rentry.login() ;
		this.jengine = jengine ;
		this.esentry = esentry ;
	}

	@Path("/run/{sid}")
	@GET @POST
	public Response runScript(@PathParam("sid") String sid, @Context HttpRequest request) throws IOException, ScriptException{
		return refWeb.runScript(sid, request) ;
	}
	
	
	@Path("/write/{sid}")
	@POST
	public Response writeFromScript(@PathParam("sid") String sid, @FormParam("eventid") String eventId, @Context HttpRequest request) throws IOException, ScriptException{
		writeScript(sid, eventId, request) ;
		
		return Response.ok().build() ;
	}

	public String writeScript(final String sid, final String eventId, @Context HttpRequest request) throws IOException, ScriptException{
		final MultivaluedMap<String, String> params = request.getDecodedFormParameters() ;
		final String content = rsession.ghostBy("/scripts/" + sid).property("content").asString() ;
		final Writer writer = new ScriptOutWriter(esentry, eventId) ;

		InstantJavaScript script = jengine.createScript(IdString.create(sid), "", new StringReader(content)) ;
		script.execAsync(new ResultHandler<Void>() {
			@Override
			public Void onSuccess(Object result, Object... args) {
				try {
					writer.write(ObjectUtil.toString(result));
				} catch (IOException e) {
				}
				IOUtil.closeQuietly(writer);
				return null;
			}

			@Override
			public Void onFail(Exception ex, Object... args) {
				try {
					writer.write("exception occured : " + ex.getMessage()) ;
					ex.printStackTrace(); 
				} catch (IOException ignore) {
				}
				IOUtil.closeQuietly(writer);
				return null;
			}
		}, writer, rsession, params, rentry, jengine) ;

		
		return null ;
	}

	
}


class ScriptOutWriter extends Writer{

	private String eventId;
	private EventSourceEntry ese;
	private StringBuilder buffer = new StringBuilder() ;

	public ScriptOutWriter(EventSourceEntry ese, String eventId) throws IOException {
		this.ese = ese ;
		this.eventId = eventId ;
 	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		buffer.append(cbuf, off, len) ;
	}

	@Override
	public void flush() throws IOException {
		ese.sendTo(eventId, buffer.toString()) ;
		buffer.setLength(0);
	}

	@Override
	public void close() throws IOException {
		flush();
		ese.closeEvent(eventId); 
	}
}
