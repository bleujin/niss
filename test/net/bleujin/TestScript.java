package net.bleujin;

import java.io.IOException;

import javax.script.ScriptException;

import junit.framework.TestCase;
import net.bleujin.rcraken.script.IdString;
import net.bleujin.rcraken.script.ResultHandler;
import net.bleujin.rcraken.script.javascript.InstantJavaScript;
import net.bleujin.rcraken.script.javascript.JScriptEngine;
import net.ion.framework.util.Debug;
import net.sf.cglib.proxy.Enhancer;

public class TestScript extends TestCase {

	public void testRunScript() throws IOException, ScriptException {
		JScriptEngine jengine = JScriptEngine.create();
		InstantJavaScript s = jengine.createScript(IdString.create("hello"), "sample",
				getClass().getResourceAsStream("sample.script"));

		s.call(new ResultHandler<Void>() {

			@Override
			public Void onSuccess(Object result, Object... args) {
				Debug.line(result) ;
				return null;
			}

			@Override
			public Void onFail(Exception ex, Object... args) {
				Debug.line(ex) ;
				return null;
			}
		}, "handle", "bleujin");
	}

	
	public void testEnhancer() throws Exception {
		 new Enhancer();
	}
}
