package net.ion.bleujin.velocity;

import java.io.File;
import java.io.StringWriter;
import java.util.Vector;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

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
	
	public void testContext() throws Exception {
		VelocityContext context = new VelocityContext();
		Vector v = new Vector();
		v.addElement("Hello");
		v.addElement("There");

		context.put("words", v.iterator() );
	}
}
