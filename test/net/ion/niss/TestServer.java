package net.ion.niss;

import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.jci.cloader.DirClassLoader;
import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;
import junit.framework.TestCase;

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
}
