package net.ion.niss.webapp.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.niss.webapp.IdString;

public class JScriptEngine {

	public final static String EntryName = "jsentry" ;
	
	private ScriptEngineManager manager ;
	private ScriptEngine sengine ;
	private ExecutorService es = new WithinThreadExecutor() ;

	private JScriptEngine(){
		this.manager = new ScriptEngineManager();
		this.sengine = manager.getEngineByName("JavaScript"); 
	}
	
	public static JScriptEngine create() {
		return new JScriptEngine();
	}

	public JScriptEngine executorService(ExecutorService es){
		this.es = es ;
		return this ;
	}
	
	public InstantJavaScript createScript(IdString lid, String explain, InputStream input) throws IOException, ScriptException{
		return createScript(lid, explain, new BufferedReader(new InputStreamReader(input, "UTF-8"))) ;
	}
	
	public InstantJavaScript createScript(IdString lid, String explain, Reader reader) throws IOException, ScriptException{
		String script = IOUtil.toStringWithClose(reader) ;

		Object compiledScript = sengine.eval(script);
		InstantJavaScript result = InstantJavaScript.create(this, explain, compiledScript) ;
	
		return result ;
	}
	
	
	public Object run(final InstantJavaScript script, final Writer writer, ExceptionHandler ehandler)  {
		try {
			return runAsync(script, writer, ehandler).get() ;
		} catch (InterruptedException e) {
			return ehandler.handle(e); 
		} catch (ExecutionException e) {
			return ehandler.handle(e); 
		}
	}

	
	
	public Future<Object> runAsync(final InstantJavaScript script, final Writer writer, ExceptionHandler ehandler)  {
		return es.submit(new Callable<Object>(){
			@Override
			public Object call() {
				try {
					Object result = ((Invocable) sengine).invokeMethod(script.compiled(), "handle", writer);
					writer.flush(); 
					writer.close(); 
					return result;
				} catch (ScriptException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch(Exception e){
					e.printStackTrace();
				}
				return null ;
			}
		}) ;
	}

	public <T> T exec(final InstantJavaScript script, ResultHandler<T> rhandler, Object... args) {
		try {
			Object result = ((Invocable) sengine).invokeMethod(script.compiled(), "handle", args);
			return rhandler.onSuccess(result, args) ;
		} catch (ScriptException e) {
			return rhandler.onFail(e, args) ;
		} catch (NoSuchMethodException e) {
			return rhandler.onFail(e, args) ;
		}
		
		
		
	}
}
