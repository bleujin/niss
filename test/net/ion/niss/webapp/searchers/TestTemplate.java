package net.ion.niss.webapp.searchers;

import java.io.FileInputStream;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.nradon.stub.StubHttpResponse;

public class TestTemplate extends TestBaseSearcher {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ss.treeContext().putAttribute(QueryTemplateEngine.EntryName, QueryTemplateEngine.create("my.craken", rentry.login())) ;
	}

	public void testViewTemplate() throws Exception {
		ss.request("/searchers/sec1/template").postParam("template", "HelloWorld").post() ;
		StubHttpResponse response = ss.request("/searchers/sec1/template").get() ;
		JsonObject json = JsonObject.fromString(response.contentsString()) ;
		assertEquals("HelloWorld", json.asString("template"));
	}
	
	
	public void testTemplateQuery() throws Exception {
		String template = IOUtil.toStringWithClose(new FileInputStream("./resource/search.template/simple_table.template"));
		
		ss.request("/searchers/sec1/template").postParam("template", template).post() ;
		
		StubHttpResponse response = ss.request("/searchers/sec1/query.template?query=*%3A*").get() ;
		Debug.line(response.contentsString());
	}
}
