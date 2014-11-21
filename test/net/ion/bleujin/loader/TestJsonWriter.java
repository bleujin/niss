package net.ion.bleujin.loader;

import java.io.StringWriter;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;

public class TestJsonWriter extends TestCase {

	public void testArray() throws Exception {
		StringWriter sw = new StringWriter();
		JsonWriter jwriter = new JsonWriter(sw) ;
		
		
		jwriter.beginArray() ;
		
		for (int i = 0; i < 5; i++) {
			jwriter.beginObject().name("idx").value(i).name("idx2").value(i).endObject() ;
		}
		
		jwriter.endArray() ;
		
		
		Debug.line(sw.toString());
	}
}
