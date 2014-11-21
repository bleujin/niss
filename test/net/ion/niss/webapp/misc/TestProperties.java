package net.ion.niss.webapp.misc;

import java.util.Map.Entry;
import java.util.Properties;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

public class TestProperties extends TestCase {

	public void testList() throws Exception {
		Properties props = System.getProperties() ;
		for (Entry<Object, Object> entry : props.entrySet()) {
			Debug.line(entry.getKey(), entry.getValue());
		}
	}
}
