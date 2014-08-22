package net.ion.niss.webapp.loaders;

import java.io.StringWriter;
import java.io.Writer;

import net.ion.framework.util.Debug;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.loaders.ExceptionHandler;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import junit.framework.TestCase;

public class TestLoadApp extends TestCase {

	public void testCreate() throws Exception {
		JScriptEngine app = JScriptEngine.create() ;
		
		InstantJavaScript script = app.createScript(IdString.create("sample_db"), "Sample From DB", JScriptEngine.class.getResourceAsStream("fromdb.txt")) ;
		
		Writer writer = new StringWriter();
		script.runAsync(writer, ExceptionHandler.DEFAULT) ;
		
		Debug.line(writer);
	}
	
}
