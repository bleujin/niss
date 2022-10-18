package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.spi.HttpRequest;

import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.radon.core.ContextParam;

@Path("/datas")
public class OpenDatasWeb {

	private REntry rentry;
	private ReadSession dsession;
	private JScriptEngine jengine;

	public OpenDatasWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine) throws IOException{
		this.rentry = rentry ;
		this.dsession = rentry.login("datas") ;
		this.jengine = jengine ;
	}
	
	
	@GET
	@Path("/{fpath : .*}")
	public String transbyTemplate(@PathParam("fpath") final String fpath, @Context HttpRequest request, @Context UriInfo uriInfo){
		String queryParam = uriInfo.getRequestUri().getQuery() ;
		
		StringWriter writer = new StringWriter() ;
		dsession.templateBy("/" + fpath).parameters(queryParam).transform(writer) ;
		
		return writer.toString() ;
	}
}
