package net.ion.niss.apps.loader;

import java.io.Writer;
import java.util.concurrent.Executor;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.infinispan.util.concurrent.WithinThreadExecutor;

public class LoadScript {

	private LoaderApp app;
	private String explain;
	private Object compiledScript ;
	LoadScript(LoaderApp app, String explain, Object compiledScript) {
		this.app = app ;
		this.explain = explain ;
		this.compiledScript = compiledScript ;
	}
	public static LoadScript create(LoaderApp app, String explain, Object compiledScript) {
		return new LoadScript(app, explain, compiledScript);
	}
	
	
	public void run(Writer writer) {
		app.run(this, writer);
	}
	public Object compiled() {
		return compiledScript;
	}



}
