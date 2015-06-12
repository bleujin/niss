package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestScriptWeb extends TestCase {

	private StubServer ss;
	private REntry rentry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(ScriptWeb.class);
		this.rentry = ss.treeContext().putAttribute(REntry.EntryName, REntry.test());
		ss.treeContext().putAttribute(JScriptEngine.EntryName, JScriptEngine.create());
		ss.treeContext().putAttribute(EventSourceEntry.EntryName, EventSourceEntry.create());
	}

	@Override
	protected void tearDown() throws Exception {
		rentry.close();
		super.tearDown();
	}

	public void testDefine() throws Exception {
		String content = IOUtil.toStringWithClose(getClass().getResourceAsStream("view.script"));
		ss.request("/scripters/test/define").postParam("content", content).post();

		ReadSession session = rentry.login();
		assertEquals(content, session.pathBy("/scripts/test").property("content").asString());
	}

	public void testListScript() throws Exception {
		String content = IOUtil.toStringWithClose(getClass().getResourceAsStream("view.script"));
		ss.request("/scripters/test0/define").postParam("content", content).post();
		ss.request("/scripters/test1/define").postParam("content", content).post();

		assertEquals(2, JsonObject.fromString(ss.request("/scripters").get().contentsString()).asJsonArray("scripters").size());
	}

	public void testRemoveScript() throws Exception {
		String content = IOUtil.toStringWithClose(getClass().getResourceAsStream("view.script"));
		ss.request("/scripters/test0/define").postParam("content", content).post();
		assertEquals(1, JsonObject.fromString(ss.request("/scripters").get().contentsString()).asJsonArray("scripters").size());

		ss.request("/scripters/test0").delete();
		assertEquals(0, JsonObject.fromString(ss.request("/scripters").get().contentsString()).asJsonArray("scripters").size());
	}

	public void testRun() throws Exception {
		String content = IOUtil.toStringWithClose(getClass().getResourceAsStream("view.script"));
		ss.request("/scripters/test0/define").postParam("content", content).post();
		StubHttpResponse response = ss.request("/scripters/test0/run").postParam("name", "bleujin").postParam("name", "hero").postParam("age", "20").post();

		JsonObject json = JsonObject.fromString(response.contentsString());
		Debug.line(json.asString("exception"));
		Debug.line(json.asString("return"));
		Debug.line(json.asString("writer"));
		Debug.line(json.asJsonArray("params"));
		assertEquals("{}", json.asString("return"));
	}
	
	public void testEditSchedule() throws Exception {
		ss.request("/scripters/test0/schedule").postParam("minute", "1").post() ;
		
		ReadSession session = rentry.login();
		assertEquals(true, session.exists("/scripts/test0/schedule")) ;
		
		ReadNode sinfo = session.pathBy("/scripts/test0/schedule") ;
		assertEquals("1", sinfo.property("minute").asString()) ;
		assertEquals("0-23", sinfo.property("hour").asString()) ;
		assertEquals("1-31", sinfo.property("day").asString()) ;
		assertEquals("1-12", sinfo.property("month").asString()) ;
		assertEquals("1-7", sinfo.property("week").asString()) ;
		assertEquals("-1", sinfo.property("matchtime").asString()) ;
		assertEquals("2014-2020", sinfo.property("year").asString()) ;
	}

	
	public void testHanja() throws Exception {
		ReadSession session = rentry.login() ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", "三星電氣") ;
				return null;
			}
		}) ;
		
		session.root().childQuery("name:三星*", true).find().debugPrint(); 
	}

}
