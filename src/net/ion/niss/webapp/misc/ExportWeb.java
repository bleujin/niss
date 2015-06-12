package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.util.TraversalStrategy;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.Webapp;
import net.ion.niss.webapp.common.ExtMediaType;
import net.ion.radon.core.ContextParam;

import com.google.common.base.Function;

@Path("/export")
public class ExportWeb implements Webapp {

	private ReadSession rsession;

	public ExportWeb(@ContextParam("rentry") REntry rentry) throws IOException {
		this.rsession = rentry.login();
	}

	@GET
	@Path("")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public StreamingOutput rootExprore() throws IOException {
		return htmlExprore("/");
	}

	@GET
	@Path("/{remain: ^[^\\.]*$}")
	@Produces(ExtMediaType.APPLICATION_JSON_UTF8)
	public StreamingOutput htmlExprore(@PathParam("remain") final String path) throws IOException {

		return new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				final JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
				jwriter.setIndent("    ");
				try {
					ReadNode find = rsession.ghostBy(path);
					find.transformer(new Function<ReadNode, Void>() {
						@Override
						public Void apply(ReadNode target) {
							target.walkChildren().asTreeChildren().includeSelf(true).strategy(TraversalStrategy.BreadthFirst).transform(new Function<Iterator<ReadNode>, Void>() {
								@Override
								public Void apply(Iterator<ReadNode> decent) {
									try {
										jwriter.beginObject();
										while (decent.hasNext()) {
											ReadNode node = decent.next();
											jwriter.jsonElement(node.fqn().toString(), node.toValueJson());
										}
										jwriter.endObject();

									} catch (IOException e) {
										e.printStackTrace();
									}
									return null;
								}
							});

							return null;
						}
					});
				} finally {
					jwriter.flush();
				}
			}
		};

	}

}
