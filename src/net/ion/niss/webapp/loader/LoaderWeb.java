package net.ion.niss.webapp.loader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ObjectId;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.radon.core.ContextParam;

@Path("/loaders")
public class LoaderWeb implements Webapp {

	private ReadSession rsession;

	public LoaderWeb(@ContextParam("rentry") REntry rentry) throws IOException {
		this.rsession = rentry.login();
	}

	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray listScript() {
		return rsession.pathBy("/loader").children().transform(new Function<Iterator<ReadNode>, JsonArray>() {
			@Override
			public JsonArray apply(Iterator<ReadNode> iter) {
				JsonArray result = new JsonArray();
				while (iter.hasNext()) {
					ReadNode node = iter.next();
					result.add(new JsonObject().put("lid", node.fqn().name()).put("explain", node.property("explain").asString()));
				}
				return result;
			}

		});

	}

	@POST
	@Path("")
	@Produces(MediaType.TEXT_PLAIN)
	public String createScript(@FormParam("explain") final String explain, @FormParam("content") final String content) {
		return "created " + rsession.tran(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				String newId = new ObjectId().toString();
				wsession.pathBy("/loader").child(newId).property("explain", explain).property("content", content).property("registered", System.currentTimeMillis());
				return newId;
			}
		});
	}

	@DELETE
	@Path("/{lid}")
	public String removeScript(@PathParam("lid") final String lid) {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/loader").removeChild(lid);
				return null;
			}
		});
		return "deleted " + lid;
	}

	@POST
	@Path("/{lid}")
	public String removeScriptForBrowser(@PathParam("lid") final String lid, @FormParam("action") String action) {
		if ("remove".equals(action)) {
			return removeScript(lid) ;
		}
		return "" ;
	}

}
