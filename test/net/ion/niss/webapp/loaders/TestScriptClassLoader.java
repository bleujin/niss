package net.ion.niss.webapp.loaders;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestCase;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.niss.cloader.DirClassLoader;
import net.ion.niss.webapp.IdString;

public class TestScriptClassLoader extends TestCase {

	
	public void testCreate() throws Exception {
		JScriptEngine js = JScriptEngine.create() ;
		
		InputStream input = new StringInputStream("new function(){ "
				+ " this.handle = function(writer) {"
				+ " writer.write('Hi bleujin') ;"
				+ " return 'Hello'; }}");
		InstantJavaScript script = js.createScript(IdString.create("test"), "", input) ;
		
		StringWriter writer = new StringWriter() ;
		Object result = js.runHandle(script, ExceptionHandler.DEFAULT, writer) ;
		
		Debug.line(result, writer);
	}
	
	public void testDir() throws Exception {
		DirClassLoader cloader = new DirClassLoader("./resource/temp") ;
		Class clz = cloader.findClass("net.ion.bleujin.cloader.Namaste") ;
		
		Object object = clz.newInstance() ;
		Method m = clz.getMethod("greeting") ;
		
		Debug.line(m.invoke(object));
	}

	public void testURLLoader() throws Exception {
		URLClassLoader cloader = new URLClassLoader(new URL[]{ new URL("jar:" + new File("./resource/temp/loader_sample_0.1.jar").toURI().toURL() + "!/")}) ;
		Class<?> clz = Class.forName("net.ion.bleujin.cloader.Namaste", false, cloader) ;
		Object object = clz.newInstance() ;
		Method m = clz.getMethod("greeting") ;
		
		Debug.line(m.invoke(object));
	}
	

	public void testClassForName() throws Exception {
		DirClassLoader cloader = new DirClassLoader("./resource/temp") ;
		Class<?> clz = Class.forName("net.ion.bleujin.cloader.Namaste", false, cloader) ;
		Object object = clz.newInstance() ;
		Method m = clz.getMethod("greeting") ;
		
		Debug.line(m.invoke(object));
	}

	public void testLoaderWithEmpty() throws Exception {
		JScriptEngine js = JScriptEngine.create("./resource/notfound") ;

		InputStream input = new StringInputStream("new function(){ "
				+ " importPackage(net.ion.bleujin.cloader) \n"
				+ " importPackage(java.lang) \n"
				+ " this.handle = function(writer) {\n"
				+ "   return 'Hello'; \n"
				+ " }"
				+ "}");
		InstantJavaScript script = js.createScript(IdString.create("test"), "", input) ;
		StringWriter writer = new StringWriter() ;
		Object result = js.runHandle(script, ExceptionHandler.DEFAULT, writer) ;
		
		Debug.line(result, writer);
	}
	
	public void testLoader() throws Exception {
		JScriptEngine js = JScriptEngine.create("./resource/temp") ;

		
		for (int i = 0; i < 2; i++) {
			InputStream input = new StringInputStream("new function(){ "
//					+ " importPackage(net.ion.bleujin.cloader) \n"
					+ " importPackage(java.lang) \n"
					+ " this.handle = function(writer) {\n"
					+ "   var clz = Class.forName('net.ion.bleujin.cloader.Namaste', false, dirloader) ; \n"
					+ "   writer.write(clz.newInstance()) ; \n"
//					+ "   writer.write(dirloader.findClass('net.ion.bleujin.cloader.Namaste').newInstance()) ; \n"
					+ "   return 'Hello'; \n"
					+ " }"
					+ "}");
			InstantJavaScript script = js.createScript(IdString.create("test"), "", input) ;
			
			StringWriter writer = new StringWriter() ;
			Object result = js.runHandle(script, ExceptionHandler.DEFAULT, writer) ;
			
			Debug.line(result, writer);
		}

	}

	public void xtestLoader2() throws Exception {
		
		JScriptEngine js = JScriptEngine.create("./resource/temp") ;
		ClassLoader cloader = js.cloader();
		Thread.currentThread().setContextClassLoader(cloader) ;

		Class<?> clz = Class.forName("net.ion.bleujin.cloader.Namaste", false, cloader) ;
		Object object = clz.newInstance() ;
		Method m = clz.getMethod("greeting") ;
		
		Debug.line(object);

		for (int i = 0; i < 1; i++) {
			InputStream input = new StringInputStream("new function(){ "
					+ " importPackage(net.ion.bleujin.cloader) \n"
					+ " importPackage(java.lang) \n"
					+ " this.handle = function(writer) {\n"
					+ "   var namaste =  new Namaste() ; \n"
					+ "   writer.write(namaste) ; \n"
					+ "   return 'Hello'; \n"
					+ " }"
					+ "}");
			InstantJavaScript script = js.createScript(IdString.create("test"), "", input) ;
			
			Object result = js.execHandle(script, ResultHandler.DEFAULT) ;
			Debug.line(result);
		}

	}

}
