package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.script.ScriptException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.jboss.resteasy.spi.HttpRequest;

import net.bleujin.rcraken.ReadSession;
import net.ion.framework.db.DBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.procedure.IUserProcedure;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.dscripts.DScriptWeb;
import net.ion.niss.webapp.dscripts.JsonStringHandler;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.radon.core.ContextParam;

@Path("/dscripts")
public class OpenDScriptWeb implements Webapp{

	private DScriptWeb refWeb;
	private REntry rentry;
	private ReadSession rsession;
	private JScriptEngine jengine;
	private EventSourceEntry esentry;
	private DBController dc;


	public OpenDScriptWeb(@ContextParam("rentry") REntry rentry, @ContextParam("jsentry") JScriptEngine jengine, @ContextParam("esentry") EventSourceEntry esentry ) throws IOException{
		this.refWeb = new DScriptWeb(rentry, jengine, esentry) ;
		this.rentry = rentry ;
		this.rsession = rentry.login() ;
		this.jengine = jengine ;
		this.esentry = esentry ;
		this.dc = rentry.scriptDBController() ;
	}

	@Path("/api/query")
	@GET @POST
	public JsonObject runScript(@FormParam("proc") String proc, @Context HttpRequest request) throws IOException, ScriptException, SQLException{
		String jsondata =  IOUtil.toStringWithClose(request.getInputStream(), "UTF-8") ;
		JsonObject json = JsonObject.fromString(jsondata) ;
		
		String procName = json.asString("proc") ;
		JsonArray jargs = json.asJsonArray("args") ;
		List<Object> args = ListUtil.newList() ;  
		jargs.forEach(jele -> args.add(jele.getAsJsonPrimitive().getValue())) ;
		
		IUserProcedure uproc = dc.createUserProcedure(procName);
		for (Object val : args) {
			if (val instanceof List) {
				uproc.addParam(toArray((List) val));
			} else {
				uproc.addParam(val);
			}
		}
		
		if (StringUtil.isNotBlank(json.asString("update"))) {
			int result = uproc.execUpdate();
			return new JsonObject().put("result", result);
		} else {
			Rows rows = uproc.execQuery();
			return new JsonStringHandler().handle(rows);
		}
		
	}
	

	private Object[] toArray(List val) {
		List<?> list = (List) val;
		if (list != null && list.size() > 0) {
			Object firstEle = list.get(0);
			if (firstEle instanceof String) {
				return list.toArray(new String[0]);
			} else if (firstEle instanceof Integer || firstEle instanceof Long) {
				Long[] result = new Long[list.size()] ;
				for(int i = 0 ; i<list.size() ; i++) {
					result[i] = Long.parseLong(list.get(i).toString()) ;
				}
				return result;
			} else if (firstEle instanceof Boolean) {
				return list.toArray(new Boolean[0]);
			}
		}
		return list.toArray(new String[0]);
	}
}


