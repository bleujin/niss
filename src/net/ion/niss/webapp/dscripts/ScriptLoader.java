package net.ion.niss.webapp.dscripts;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.db.FileAlterationMonitor;
import net.bleujin.rcraken.script.javascript.ScriptJDK;
import net.ion.framework.db.Rows;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

public class ScriptLoader {

	private ScriptEngine sengine;
	private Map<String, Object> packages = MapUtil.newCaseInsensitiveMap();
	private FileAlterationMonitor monitor;

	public ScriptLoader(ReadSession rsession) {
		ScriptEngineManager manager = new ScriptEngineManager();
		this.sengine = manager.getEngineByName("JavaScript");
		sengine.put("session", rsession);
	}

	public static ScriptLoader create(ReadSession rsession) {
		return new ScriptLoader(rsession);
	}

	public void loadPackageScript(String packName, String script)  {
		try {
			packages.put(packName, sengine.eval(ScriptJDK.trans(script)));
		} catch (ScriptException e) {
			throw new IllegalStateException(e) ;
		}
	}

	public Map<String, Object> packages() {
		return Collections.unmodifiableMap(packages);
	}

	public Rows execQuery(String uptName, Object... params) throws SQLException {
		Object result = callFn(uptName, params);
		if (Rows.class.isInstance(result))
			return (Rows) result;

		throw new IllegalStateException("illegal return type");
	}

	public Object callFn(String uptName, Object... params) throws SQLException{
		try {
			String packName = StringUtil.substringBefore(uptName, "@");
			String fnName = StringUtil.substringAfter(uptName, "@");

			Object compiledScript = packages.get(packName);
			if (compiledScript == null)
				throw new SQLException("not found package");

			Object result = ((Invocable) sengine).invokeMethod(compiledScript, fnName, params);
			return result;
		} catch (ScriptException e) {
			throw new SQLException(e);
		} catch (NoSuchMethodException e) {
			throw new SQLException(e);
		}
	}

	public int execUpdate(String uptName, Object... params) throws SQLException{
		Object result = callFn(uptName, params);
		if (result == null) return 0 ;
		if (Integer.class.isInstance(result)) return (Integer) result ;
		if (Double.class.isInstance(result)) return ((Double)result).intValue() ;

		throw new IllegalStateException("illegal return type");
	}

	public void removePackageScript(String packName) {
		packages.remove(packName) ;
	}
}
