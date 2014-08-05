package net.ion.niss.apps.loader;

import java.io.Writer;
import java.util.concurrent.Executor;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.infinispan.util.concurrent.WithinThreadExecutor;

public class Loader {

	private String script ;
	private Executor exec ;
	private Loader(String script) {
		this.script = script ;
	}

	public static Loader fromScript(String script) {
		return new Loader(script) ;
	}

	public Loader executor(Executor exec) {
		this.exec = exec ;
		return this ;
	}

	public void run(Writer writer) throws ScriptException, NoSuchMethodException {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine sengine = manager.getEngineByName("JavaScript");

		Object pack = sengine.eval(script);
		Object result = ((Invocable) sengine).invokeMethod(pack, "handle", writer);

		// TODO Auto-generated method stub
		
	}

}
