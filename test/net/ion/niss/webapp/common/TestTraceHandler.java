package net.ion.niss.webapp.common;

import junit.framework.TestCase;
import net.ion.framework.util.InfinityThread;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.handler.authentication.InMemoryPasswords;
import net.ion.radon.aclient.ClientConfig;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Realm;
import net.ion.radon.aclient.Realm.RealmBuilder;
import net.ion.radon.aclient.RequestBuilder;
import net.ion.radon.aclient.Response;
import net.ion.radon.core.let.PathHandler;

import org.jboss.netty.handler.codec.http.HttpMethod;

public class TestTraceHandler extends TestCase {

	private NewClient nc;
	private Radon radon;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		REntry rentry = REntry.test();
		this.radon = RadonConfiguration.newBuilder(9800)
				.add(new MyAuthenticationHandler(new InMemoryPasswords().add("bleujin", "1")))
				.add(new TraceHandler(rentry))
				.add(new PathHandler(DummyLet.class))
				.staleConnectionTimeout(200000)
				.startRadon();

		radon.getConfig().getServiceContext().putAttribute(REntry.EntryName, rentry);
		this.nc = NewClient.create() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		radon.stop() ;
		nc.close();
		super.tearDown();
	}

	public void testParamWhenTrace() throws Exception {

		NewClient nc = NewClient.create(ClientConfig.newBuilder().setRequestTimeoutInMs(200000).build());
		Realm realm = new RealmBuilder().setPrincipal("bleujin").setPassword("1").build() ;
		Response resposne = nc.prepareRequest(new RequestBuilder().setRealm(realm).setUrl("http://localhost:9800/adc/def").setMethod(HttpMethod.POST).build()).execute().get() ;
		
		
		
		nc.close();
		new InfinityThread().startNJoin();
	}
}
