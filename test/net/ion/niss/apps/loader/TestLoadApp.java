package net.ion.niss.apps.loader;

import java.io.StringWriter;
import java.io.Writer;

import net.ion.framework.util.Debug;
import net.ion.niss.apps.IdString;
import junit.framework.TestCase;

public class TestLoadApp extends TestCase {

	public void testCreate() throws Exception {
		LoaderApp app = LoaderApp.create() ;
		
		LoadScript script = app.createScript(IdString.create("sample_db"), "Sample From DB", LoaderApp.class.getResourceAsStream("fromdb.txt")) ;
		
		Writer writer = new StringWriter();
		script.run(writer) ;
		
		Debug.line(writer);
	}
	
}
