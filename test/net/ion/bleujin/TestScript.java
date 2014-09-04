package net.ion.bleujin;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import net.ion.framework.util.IOUtil;
import junit.framework.TestCase;

public class TestScript extends TestCase {

	
	public void testRun() throws Exception {
		ScriptEngineManager em = new ScriptEngineManager() ;
		ScriptEngine en = em.getEngineByName("JavaScript") ;
		
		en.eval("java.lang.System.out.println(\"Hello\")");
	}
	
	public void testArgument() throws Exception {
		ScriptEngineManager em = new ScriptEngineManager() ;
		ScriptEngine en = em.getEngineByName("JavaScript") ;

		Bindings bind = new SimpleBindings();
		bind.put("name", "bleujin") ;
		en.eval("java.lang.System.out.println(name)", bind);
	}
	
	public void testCompile() throws Exception {
		ScriptEngineManager em = new ScriptEngineManager() ;
		ScriptEngine en = em.getEngineByName("JavaScript") ;

		Object compiled = en.eval(IOUtil.toStringWithClose(getClass().getResourceAsStream("script.txt"))) ;
		((Invocable)en).invokeMethod(compiled, "handle") ;
	}
	
}
