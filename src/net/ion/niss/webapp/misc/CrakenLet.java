package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import net.bleujin.rcraken.Property;
import net.bleujin.rcraken.Property.PType;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.radon.core.ContextParam;

@Path("/craken")
public class CrakenLet implements Webapp{

	private ReadSession rsession;
	private Engine engine;

	public CrakenLet(@ContextParam("rentry") REntry rentry) throws IOException{
		this.rsession = rentry.login() ;
		this.engine = rsession.workspace().parseEngine();
	}

	@GET
	@Path("")
	@Produces(ExtMediaType.TEXT_HTML_UTF8)
	public Response rootExprore() throws IOException{
		return htmlExprore("/", "") ;
	}

	
	@GET
	@Path("/{remain: ^[^\\.]*$}")
	@Produces(ExtMediaType.TEXT_HTML_UTF8)
	public Response htmlExprore(@PathParam("remain") final String path, @QueryParam("command") String command) throws IOException{
		ReadNode find = rsession.pathBy(path) ;
		if (! find.exist()) return Response.status(404).build() ;
		String result = engine.transform(IOUtil.toStringWithClose(getClass().getResourceAsStream("craken.tpl")), MapUtil.<String, Object>create("self", find)) ;
		
		if ("DELETE".equals(command)) {
			rsession.tran( wsession -> {
				wsession.pathBy(path).removeSelf() ;
			}) ;
		}
		
		
		return Response.ok(result).build() ;
	}
	
	
	@GET
	@Path("/{remain: .*}.{pid}")
	public Response property(@PathParam("remain") String path, @PathParam("pid") String pid) throws IOException{
		ReadNode find = rsession.pathBy(path) ;
		if (! find.exist()) return Response.status(404).build() ;

		
		Property pvalue = find.property(pid) ;
		if (pvalue.type() == PType.Lob){
			InputStream input = pvalue.asStream() ;
			return Response.ok(input, ExtMediaType.TEXT_PLAIN_UTF8).build() ;
		} else {
			return Response.ok(pvalue.asString(), ExtMediaType.TEXT_HTML_UTF8).build() ;
		}
	}
}
