package net.ion.niss.apps.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.IOUtil;
import net.ion.niss.apps.IdString;

import org.infinispan.manager.DefaultCacheManager;

public class LoaderApp {

	private final RepositoryImpl r ;
	private ScriptEngineManager manager ;
	private ScriptEngine sengine ;

	private LoaderApp(RepositoryImpl r){
		this.r = r ;
		this.manager = new ScriptEngineManager();
		this.sengine = manager.getEngineByName("JavaScript"); 
	}
	
	public static LoaderApp create() throws IOException {
		LoaderApp result = new LoaderApp(createSolo());
		
		return result ;
	}
	
	private static RepositoryImpl createSolo() throws IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("admin", ISearcherWorkspaceConfig.create().location("./resource/admin"));
		return r;
	}


	public LoadScript createScript(IdString lid, String explain, InputStream input) throws IOException, ScriptException{
		String script = IOUtil.toStringWithClose(input) ;

		Object compiledScript = sengine.eval(script);
		LoadScript result = LoadScript.create(this, explain, compiledScript) ;
	
		
		return result ;
	}
	
	
	public void run(LoadScript script, Writer writer)  {
		try {
			Object result = ((Invocable) sengine).invokeMethod(script.compiled(), "handle", writer);
		} catch (ScriptException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

}
