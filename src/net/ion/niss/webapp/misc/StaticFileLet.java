package net.ion.niss.webapp.misc;

import java.io.File;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import net.ion.radon.core.ContextParam;
import net.ion.radon.core.let.FileResponseBuilder;


@Path("")
public class StaticFileLet {

	
	private File homeDir;

	public StaticFileLet(@DefaultValue("./webapps/admin") @ContextParam("staticHome") String staticHome){
		this.homeDir = new File(staticHome) ;
	}
	
	
	@Path("/{remain:.*}.{ext}")
	@GET
	public Response viewFile(@PathParam("remain") String path, @PathParam("ext") String ext){
		File file = new File(homeDir, path + "." + ext) ;
		
		if (file.exists()){
			return new FileResponseBuilder(file).build() ;
		} else {
			return Response.status(404).build() ;
		}
		
		
	}
	
}
