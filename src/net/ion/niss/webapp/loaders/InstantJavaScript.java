package net.ion.niss.webapp.loaders;

import java.io.Writer;
import java.util.concurrent.Future;

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

	public Object compiled() {
		return compiledScript;
	}
	
	public <T> T exec(ResultHandler<T> rhandler, Object... args) {
		return app.execHandle(this, rhandler, args) ;
	}

	public <T> Future<T> execAsync(ResultHandler<T> rhandler, Object... args) {
		return app.runAsyncHandle(this, rhandler, args);
	}

}
