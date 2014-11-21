package net.ion.niss.webapp.misc;

import java.io.StringWriter;
import java.io.Writer;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.GsonBuilder;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;

public class TestSystemInfo extends TestCase {

	public void testInfo() throws Exception {
		JsonObject json =  new SystemInfo().list() ;
		
		Writer writer = new StringWriter() ;
		new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
		
		Debug.line(writer);
		
	}
}
