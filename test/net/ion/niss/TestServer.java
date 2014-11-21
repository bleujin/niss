package net.ion.niss;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.niss.config.builder.ConfigBuilder;

public class TestServer extends TestCase {

	
	public void testRun() throws Exception {
		NissServer.create(ConfigBuilder.createDefault(9000).build()).start() ;
		new InfinityThread().startNJoin(); 
	}
	
	public void testLoader() throws Exception {
		ClassLoader current = getClass().getClassLoader() ;
		
		Debug.line(current, current.getParent(), current.getParent().getParent());
		
		
		// new DirClassLoader(homeDirectory).addDirectory(additionalDirectory);
	}
	
	public void testCanoName() throws Exception {
		Debug.debug(getClass().getCanonicalName()) ;
	}
}
