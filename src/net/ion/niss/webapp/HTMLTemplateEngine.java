package net.ion.niss.webapp;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.util.WebUtil;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.handler.TemplateEngine;
import net.ion.nradon.handler.authentication.BasicAuthenticationHandler;
import net.ion.radon.core.TreeContext;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class HTMLTemplateEngine implements TemplateEngine {

	private VelocityContext vcontext;
	private Charset utf8;
	private VelocityEngine ve;
	private ReadSession rsession;
	private REntry rentry;

	public HTMLTemplateEngine(TreeContext tcontext) throws IOException {
		this.utf8 = Charset.forName("UTF-8") ;
		this.ve = new VelocityEngine();
		this.vcontext = new VelocityContext() ;
		vcontext.put(TreeContext.class.getCanonicalName(), tcontext) ;
		this.rentry = tcontext.getAttributeObject(REntry.EntryName, REntry.class) ;
		this.rsession = rentry.login() ;

		ve.setProperty("resource.loader", "file");
		ve.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		ve.setProperty("file.resource.loader.path", new File("./").getCanonicalPath() + "/webapps/admin");
		ve.setProperty("file.resource.loader.cache", "true");
		ve.setProperty("file.resource.loader.modificationCheckInterval", "5");
		ve.setProperty("output.encoding", "UTF-8");
		ve.setProperty("input.encoding", "UTF-8");

		ve.init();
	}

	
	private String[] template_html = new String[]{"/index.html", "/indexers.html", "/"} ;
	
	@Override
	public byte[] process(byte[] template, String templatePath, Object arg) throws RuntimeException {

		
		if (WebUtil.isStaticResource(templatePath)) {
			return template ;
		}
		
		//if (ArrayUtil.contains(template_html, templatePath)) {
		if (templatePath.equals("/")) templatePath = "/index.html" ;
		if (templatePath.endsWith(".html")) {
			HttpRequest request = (HttpRequest) arg;

			if (! ve.resourceExists(templatePath)) return template ;
			
			Template tpl = ve.getTemplate(templatePath, "UTF-8");
			
			
			StringWriter sw = new StringWriter();
			VelocityContext vc = new VelocityContext(this.vcontext) ;
			
			vc.put("request", request) ;
			vc.put("rsession", rsession) ;
			
			if (templatePath.equals("/index.html")) {
				ReadChildren children = rsession.ghostBy("/traces/" + request.data(BasicAuthenticationHandler.USERNAME)).children() ;
				vc.put("traces", children) ;
			} 
			
			
			
			tpl.merge(vc, sw);
			return sw.toString().getBytes(utf8) ;
		}
		return template;
	}

}
