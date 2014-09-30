package net.ion.niss;

import net.ion.framework.util.InfinityThread;
import junit.framework.TestCase;

public class TestServer extends TestCase {

	
	public void testRun() throws Exception {
		NissServer.create(9000).start() ;
		new InfinityThread().startNJoin(); 
	}
}
