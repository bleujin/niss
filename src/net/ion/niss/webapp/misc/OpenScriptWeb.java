package net.ion.niss.webapp.misc;

import java.io.IOException;

import javax.script.ScriptException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;

import net.bleujin.rcraken.ReadSession;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.scripters.ScriptWeb;
import net.ion.radon.core.ContextParam;

@Path("/script")
public class OpenScriptWeb implements Webapp{

	private ScriptWeb refWeb;
	private REntry rentry;
	private ReadSession rsession;
	private JScriptEngine jengine;
	private EventSourceEntry esentry;

	public OpenScriptWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine, @ContextParam("esentry") EventSourceEntry esentry ) throws IOException{
		this.refWeb = new ScriptWeb(rentry, jengine, esentry) ;
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
	
	
}


