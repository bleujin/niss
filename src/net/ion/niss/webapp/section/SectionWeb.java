package net.ion.niss.webapp.section;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.Webapp;

@Path("/sections")
public class SectionWeb implements Webapp{

	@POST
	@Path("/{sid}")
	public String mergeSection(@FormParam("collection") String[] cols){
		return StringUtil.join(cols, ",") ;
	}
}
