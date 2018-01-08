package net.ion.bleujin.velocity;

import java.io.File;
import java.io.StringWriter;
import java.util.Vector;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.SystemLogChute;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;

import junit.framework.TestCase;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.searchers.CrakenResourceRepository;
public class TestVelocity extends TestCase {

	
	public void testFirst() throws Exception {
		VelocityEngine ve = new VelocityEngine();

		ve.setProperty("resource.loader", "file");
		ve.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		ve.setProperty("file.resource.loader.path", new File("./").getCanonicalPath() + "/test/net/ion/bleujin/velocity");
		ve.setProperty("file.resource.loader.cache", "true");
		ve.setProperty("file.resource.loader.modificationCheckInterval", "5");
		ve.init();
		
		
		
		Debug.line( new File("./").getCanonicalPath() );
		Template t = ve.getTemplate("hello.txt");
		
		StringWriter sw = new StringWriter();
		t.merge(new VelocityContext(), sw);
		Debug.line(sw);
	}
	
	public void testResourceLoader() throws Exception {
		VelocityEngine ve = new VelocityEngine();

		ve.setProperty("resource.loader", "file");
		ve.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		ve.setProperty("file.resource.loader.path", new File("./").getCanonicalPath() + "/webapps/admin");
		ve.setProperty("file.resource.loader.cache", "true");
		ve.setProperty("file.resource.loader.modificationCheckInterval", "5");
		ve.init();
		
		assertEquals(true, ve.resourceExists("/index.html")) ;
		assertEquals(true, ve.resourceExists("/icss/indexers.html")) ;
	}
	
	
	public void testMyResourceLoader() throws Exception {
		
		REntry r = REntry.test() ;
		ReadSession rsession = r.login() ;
		
		CrakenResourceRepository repo = new CrakenResourceRepository(rsession);
		repo.putStringResource("/path/hello.vm", "Hi, ${username}... this is some template!");
		
		VelocityEngine ve = newStringEngine("my.repo", repo);
		assertEquals(true, ve.resourceExists("/path/hello.vm")) ;
		
		Template t = ve.getTemplate("/path/hello.vm");
		StringWriter sw = new StringWriter();
		VelocityContext vc = new VelocityContext();
		vc.put("username", "bleujin") ;
		t.merge(vc, sw);
		Debug.line(sw);
		
		r.repository().shutdown() ;
	}
	
	private VelocityEngine newStringEngine(String repoName, CrakenResourceRepository repo) {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(Velocity.RESOURCE_LOADER, "string");
		engine.addProperty("string.resource.loader.class", StringResourceLoader.class.getName());
		engine.addProperty("string.resource.loader.repository.name", repoName);
		engine.addProperty("string.resource.loader.repository.static", "false");
		engine.addProperty("string.resource.loader.modificationCheckInterval", "1");
		engine.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, SystemLogChute.class.getName());

		engine.setApplicationAttribute(repoName, repo);
		return engine;
	}
	
	
	
	public void testContext() throws Exception {
		VelocityContext context = new VelocityContext();
		Vector v = new Vector();
		v.addElement("Hello");
		v.addElement("There");

		context.put("words", v.iterator() );
	}
}
