package net.ion.niss.webapp.loaders;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllLoader extends TestCase{

	public static Test suite() {
		TestSuite ts = new TestSuite("All Loader");
		
		ts.addTestSuite(TestLoaderWeb.class) ;
		ts.addTestSuite(TestSampleLoader.class);
		ts.addTestSuite(TestScriptClassLoader.class);
		return ts ;
	}
}
