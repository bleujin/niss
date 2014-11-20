package net.ion.niss.webapp.misc;

import java.io.IOException;

import javax.script.ScriptException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.radon.core.ContextParam;

import org.jboss.resteasy.spi.HttpRequest;

@Path("/script")
public class OpenScriptWeb implements Webapp{

	private ScriptWeb refWeb;
	public OpenScriptWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine ) throws IOException{
		this.refWeb = new ScriptWeb(rentry, jengine) ;
	}
	

	@Path("/run/{sid}")
	@GET @POST
	public Response runScript(@PathParam("sid") String sid, @Context HttpRequest request) throws IOException, ScriptException{
		return refWeb.runScript(sid, request) ;
	}
}
