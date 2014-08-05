package net.ion.niss.apps.loader;

import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import org.infinispan.util.concurrent.WithinThreadExecutor;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

public class TestFromCrawler extends TestCase {


	private ScriptEngine sengine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testRunWithScript() throws Exception {
		String script = IOUtil.toStringWithClose(getClass().getResourceAsStream("crawler.script"));
		Writer writer = new OutputStreamWriter(System.out, "EUC-KR");
		
		Loader.fromScript(script).executor(new WithinThreadExecutor()).run(writer) ;
	}
	
	public void testTo() throws Exception {
		Debug.line("한글");
	}

}
