package net.ion.niss.webapp.loaders;

import java.io.Writer;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.ion.nsearcher.search.Searcher;

import org.infinispan.util.concurrent.WithinThreadExecutor;

public class InstantJavaScript {

	private JScriptEngine app;
	private String explain;
	private Object compiledScript ;
	InstantJavaScript(JScriptEngine app, String explain, Object compiledScript) {
		this.app = app ;
		this.explain = explain ;
		this.compiledScript = compiledScript ;
	}
	public static InstantJavaScript create(JScriptEngine app, String explain, Object compiledScript) {
		return new InstantJavaScript(app, explain, compiledScript);
	}

	public Future<Object> runAsync(Writer writer, ExceptionHandler ehandler) {
		return app.runAsync(this, writer, ehandler);
	}
	
	public Object run(Writer writer, ExceptionHandler ehandler) {
		return app.run(this, writer, ehandler);
	}
	
	public Object compiled() {
		return compiledScript;
	}
	public <T> T exec(ResultHandler<T> rhandler, Object... args) {
		return app.exec(this, rhandler, args) ;
	}



}
