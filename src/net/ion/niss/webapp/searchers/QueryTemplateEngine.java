package net.ion.niss.webapp.searchers;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import net.ion.craken.node.ReadSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;

public class QueryTemplateEngine {

	public final static String EntryName = "qtemplate" ;
	private final VelocityEngine ve ;
	private VelocityContext root = new VelocityContext() ;

	public QueryTemplateEngine(String name, CrakenResourceRepository repo) {
		this.ve = newStringEngine(name, repo) ;
	}

	public static QueryTemplateEngine create(String name, ReadSession rsession) {
		CrakenResourceRepository repo = new CrakenResourceRepository(rsession);
		repo.putStringResource("/path/hello.vm", "Hi, ${username}... this is some template!");
		
		return new QueryTemplateEngine(name, repo);
	} 

	private VelocityEngine newStringEngine(String repoName, CrakenResourceRepository repo) {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(Velocity.RESOURCE_LOADER, "string");
		engine.addProperty("string.resource.loader.class", StringResourceLoader.class.getName());
		engine.addProperty("string.resource.loader.repository.name", repoName);
		engine.addProperty("string.resource.loader.repository.static", "false");
		engine.addProperty("string.resource.loader.modificationCheckInterval", "1");
//		engine.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, SystemLogChute.class.getName());

		engine.setApplicationAttribute(repoName, repo);
		return engine;
	}
	
	public boolean hasTemplate(String resourceName){
		return ve.resourceExists(resourceName) ;
	}
	
	public void merge(String resourceName, Map<String, Object> context, Writer writer) throws IOException{
		Template template = ve.getTemplate(resourceName) ;
		if (template == null) {
			writer.write("has not template");
			return;
		}
		
		VelocityContext vc = new VelocityContext(root) ;
		for (Entry<String, Object> entry : context.entrySet()) {
			vc.put(entry.getKey(), entry.getValue()) ;
		}
		template.merge(vc, writer);
	}
	
}
