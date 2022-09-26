package net.ion.niss.webapp.loaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.ion.framework.util.WithinThreadExecutor;
import net.ion.jci.monitor.AbstractListener;
import net.ion.jci.monitor.FileAlterationMonitor;
import net.ion.niss.cloader.DirClassLoader;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.util.ScriptJDK;
import net.ion.radon.cload.cloader.OuterClassLoader;

public class JScriptEngine {

	public final static String EntryName = "jsentry" ;
	
	private ScriptEngineManager manager ;
	private ScriptEngine sengine ;
	private ExecutorService es = new WithinThreadExecutor() ;
	private ClassLoader cloader;
	private Log log = LogFactory.getLog(JScriptEngine.class) ;

	private JScriptEngine(ClassLoader cloader){
		this.manager = new ScriptEngineManager();
		this.sengine = manager.getEngineByName("JavaScript"); 
		Bindings bindings = new SimpleBindings();
		bindings.put("dirloader", cloader) ;
		sengine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
		this.cloader = cloader ;
	}
	
	public static JScriptEngine create() {
		return new JScriptEngine(Thread.currentThread().getContextClassLoader());
	}
	
	public static JScriptEngine create(String libPath) throws Exception{
		return create(libPath, null, false) ;
	}

	public static JScriptEngine create(String libPath, ScheduledExecutorService ses, boolean reload) throws Exception{
		final File libDir = new File(libPath);
		final Log l = LogFactory.getLog(JScriptEngine.class) ;
		if (libDir.exists() && libDir.isDirectory()) {
			ClassLoader cloader = new DirClassLoader(libPath) ;
			if (reload){
				final OuterClassLoader classloader = new OuterClassLoader(cloader);
				FileAlterationObserver fo = new FileAlterationObserver(libDir) ;
				fo.addListener(new AbstractListener() {
					@Override
					public void onFileChange(File file) {
						try {
							classloader.change(new DirClassLoader(libDir));
						} catch (IOException ignore) {
							ignore.printStackTrace();
						}
					}
				});
				FileAlterationMonitor fam = new FileAlterationMonitor(3000, ses, fo);
				fam.start();
			}
			return new JScriptEngine(cloader) ;
		} else {
			l.info("Has not Extension libPath : " + libPath);
			return create() ;
		}
	}
	
	ClassLoader cloader(){
		return cloader ;
	}

	
	public JScriptEngine executorService(ExecutorService es){
		this.es = es ;
		return this ;
	}
	
	public ExecutorService executor(){
		return es ;
	}
	
	public InstantJavaScript createScript(IdString lid, String explain, InputStream input) throws IOException, ScriptException{
		return createScript(lid, explain, new BufferedReader(new InputStreamReader(input, "UTF-8"))) ;
	}
	
	public InstantJavaScript createScript(IdString lid, String explain, Reader reader) throws IOException, ScriptException{
		String script = ScriptJDK.trans(reader) ;

		Object compiledScript = sengine.eval(script);
		InstantJavaScript result = InstantJavaScript.create(this, explain, compiledScript) ;
	
		return result ;
	}
	

	
	<T> Future<T> runAsyncHandle(final InstantJavaScript script, final ResultHandler<T> rhandler, final Object... args)  {
		return es.submit(new Callable<T>(){
			@Override
			public T call() {
				try {
					Object result = ((Invocable) sengine).invokeMethod(script.compiled(), "handle", args);
					return rhandler.onSuccess(result, args);
				} catch (ScriptException e) {
					return rhandler.onFail(e, args) ;
				} catch (NoSuchMethodException e) {
					return rhandler.onFail(e, args) ;
				} catch(Exception e){
					return rhandler.onFail(e, args) ;
				} 
			}
		}) ;
	}

	<T> T execHandle(final InstantJavaScript script, ResultHandler<T> rhandler, Object... args) {
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
