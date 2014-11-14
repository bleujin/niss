package net.ion.niss.webapp.misc;

import javax.script.ScriptEngine;

import net.ion.craken.node.ReadSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;
import junit.framework.TestCase;

public class TestScriptWeb extends TestCase {

	
	private StubServer ss;
	private REntry rentry;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(ScriptWeb.class) ;
		this.rentry = ss.treeContext().putAttribute(REntry.EntryName, REntry.test()) ;
		ss.treeContext().putAttribute(JScriptEngine.EntryName, JScriptEngine.create()) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		rentry.close(); 
		super.tearDown();
	}
	
	public void testDefine() throws Exception {
		String content = IOUtil.toStringWithClose(getClass().getResourceAsStream("test.script"));
		ss.request("/scripts/define/test").postParam("content", content).post() ; 
		
		ReadSession session = rentry.login() ;
		assertEquals(content, session.pathBy("/scripts/test").property("content").asString()) ;
	}
	
	public void testListScript() throws Exception {
		String content = IOUtil.toStringWithClose(getClass().getResourceAsStream("test.script"));
		ss.request("/scripts/define/test0").postParam("content", content).post() ;
		ss.request("/scripts/define/test1").postParam("content", content).post() ;
		
		assertEquals(2, JsonObject.fromString(ss.request("/scripts").get().contentsString()).asJsonArray("scripts").size()) ;
	}
	
	public void testRemoveScript() throws Exception {
		String content = IOUtil.toStringWithClose(getClass().getResourceAsStream("test.script"));
		ss.request("/scripts/define/test0").postParam("content", content).post() ;
		assertEquals(1, JsonObject.fromString(ss.request("/scripts").get().contentsString()).asJsonArray("scripts").size()) ;

		ss.request("/scripts/remove/test0").delete() ;
		assertEquals(0, JsonObject.fromString(ss.request("/scripts").get().contentsString()).asJsonArray("scripts").size()) ;
	}

	
	public void testRun() throws Exception {
		String content = IOUtil.toStringWithClose(getClass().getResourceAsStream("test.script"));
		ss.request("/scripts/define/test0").postParam("content", content).post() ;
		StubHttpResponse response = ss.request("/scripts/run/test0").postParam("name", "bleujin").postParam("name", "hero").postParam("age", "20").post() ;
		
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		Debug.line(json.asString("exception"));
		Debug.line(json.asString("return"));
		Debug.line(json.asString("writer"));
		Debug.line(json.asJsonArray("params"));
	}
	

}
