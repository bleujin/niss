package net.ion.niss.webapp;

import org.infinispan.util.InfinispanCollections;

import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;
import net.ion.radon.core.let.PathHandler;
import junit.framework.TestCase;

public class TestCollectionWeb extends TestCase  {

	public void testStatus() throws Exception {
		StubServer ss = StubServer.create(CollectionWeb.class) ;
		
		StubHttpResponse response = ss.request("/collection/col1/status").get() ;
		Debug.line(response.contentsString());
	}
	
	public void xtestServer() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(9500).add(new PathHandler(CollectionWeb.class).prefixURI("/admin")).start().get() ;
		new InfinityThread().startNJoin(); 
	}
}
