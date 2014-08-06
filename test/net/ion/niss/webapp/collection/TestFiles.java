package net.ion.niss.webapp.collection;

import javax.ws.rs.core.MediaType;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.niss.apps.collection.CollectionApp;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;
import net.ion.radon.core.let.PathHandler;

import org.jboss.resteasy.util.HttpHeaderNames;

public class TestFiles extends TestCase {

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(CollectionWeb.class) ;
		ss.treeContext().putAttribute(CollectionApp.class.getSimpleName(), CollectionApp.create()) ;
	}
	
	public void testFileList() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/files").get() ;
		
		assertEquals(MediaType.APPLICATION_JSON, response.header(HttpHeaderNames.CONTENT_TYPE));
		Debug.line(response.contentsString());
	}
	
	public void testFile() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/files/Stopword.txt").get() ;
		assertEquals(200, response.status());
		Debug.line(response.contentsString());
	}
	
	public void testUpdateFile() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/files/Stopword.txt").postParam("content", "bleujin hero").post() ;
		assertEquals(200, response.status());
		Debug.line(response.contentsString());
	}
	
	
	
	
}
