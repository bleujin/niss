package net.ion.niss.apps.misc;

import java.util.Properties;
import java.util.Map.Entry;

import net.ion.framework.util.Debug;

import org.apache.commons.lang.SystemUtils;

import junit.framework.TestCase;

public class TestProperties extends TestCase {

	public void testList() throws Exception {
		Properties props = System.getProperties() ;
		for (Entry<Object, Object> entry : props.entrySet()) {
			Debug.line(entry.getKey(), entry.getValue());
		}
	}
}
