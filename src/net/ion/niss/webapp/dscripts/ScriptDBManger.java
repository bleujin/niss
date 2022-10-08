package net.ion.niss.webapp.dscripts;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.db.CrakenManager;
import net.bleujin.rcraken.db.CrakenScript;
import net.bleujin.rcraken.db.CrakenScriptManager;
import net.bleujin.rcraken.db.CrakenUserProcedure;
import net.bleujin.rcraken.db.CrakenUserProcedureBatch;
import net.bleujin.rcraken.db.CrakenUserProcedures;
import net.ion.framework.db.Rows;

public class ScriptDBManger extends CrakenManager {

	private final ScriptLoader sloader;
	private ScriptDBManger(ScriptLoader sloader) {
		this.sloader = sloader ;
	}

	public static ScriptDBManger create(ReadSession rsession) throws IOException {
		ScriptLoader sloader = new ScriptLoader(rsession) ;
		
		return new ScriptDBManger(sloader) ;
	}

	
	public void loadPackage(String packName, String scontent) {
		sloader.loadPackageScript(packName, scontent);
	}
	
	@Override
	public Rows queryBy(CrakenUserProcedure cupt) throws Exception {
		return sloader.execQuery(cupt.getProcName(), cupt.getParams().toArray(new Object[0])) ;
	}

	@Override
	public int updateWith(CrakenUserProcedure cupt) throws Exception {
		return sloader.execUpdate(cupt.getProcName(), cupt.getParams().toArray(new Object[0])) ;
	}

	@Override
	public int updateWith(CrakenUserProcedureBatch cupt) throws Exception {
		return sloader.execUpdate(cupt.getProcName(), cupt.getParams().toArray(new Object[0])) ;
	}

	@Override
	public int updateWith(CrakenUserProcedures cupts) throws Exception {
		throw new UnsupportedOperationException("if you must do it, call bleujin") ;
	}

	public void removePackage(String packName) {
		sloader.removePackageScript(packName) ;
	}
}
