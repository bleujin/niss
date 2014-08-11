package net.ion.niss.webapp.loader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.FileUtil;
import net.ion.niss.apps.Store;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

@Path("/loader")
public class LoaderWeb implements Webapp{

	private ReadSession r;
	public LoaderWeb(@ContextParam("Store") Store s) throws IOException{
		this.r = s.login() ;
	}
	

	
	@POST
	@Path("/{sid}")
	public String createScript(@PathParam("sid") String sid, @FormParam("explain") String explain, @FormParam("scontent") String scontent){
		
		return "working" ;
	}
	
	@GET
	@Path("")
	public JsonArray listFile() throws IOException{
		Collection<File> sfiles = FileUtil.listFiles(new File("./resource/loader"), new String[]{"script"}, false) ;
		JsonArray result = new JsonArray() ;
		for (File file : sfiles) {
			List<String> contents = FileUtil.readLines(file, "UTF-8") ;
			result.add(new JsonObject().put("name", file.getName()).put("content", contents.size() > 0 ? contents.get(0) : "")) ;
		}
		
		return result ;
	}
}
