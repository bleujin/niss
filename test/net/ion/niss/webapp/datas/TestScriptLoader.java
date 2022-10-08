package net.ion.niss.webapp.datas;

import java.io.File;
import java.sql.SQLException;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.db.DBController;
import net.ion.framework.db.Rows;
import net.ion.framework.util.FileUtil;
import net.ion.niss.webapp.dscripts.ScriptDBManger;

public class TestScriptLoader extends TestCase {
	
	
	private DBController dc;
	private Craken c ;

	protected void setUp() throws Exception {
		c = CrakenConfig.mapMemory().build() ;
		c.start() ;
		ReadSession rsession = c.login("dataworkspace") ;
		
		ScriptDBManger sdbm = ScriptDBManger.create(rsession) ;
		sdbm.loadPackage("board", FileUtil.readFileToString(new File("./resource/dscript/board.script")));
		
		this.dc = new DBController("craken", sdbm);
		dc.initSelf() ;
	}
	
	protected void tearDown() throws Exception {
		dc.destroySelf();
		c.shutdown(); 
	}
	
	
	public void testCall() throws SQLException {
		dc.createUserProcedure("board@createPostWith(?,?,?,?)").addParam("tboard").addParam("this is subject").addParam("hello content").addParam("bleujin").execUpdate() ;
		
		Rows rows = dc.createUserProcedure("board@listPostBy(?,?,?)").addParam("tboard").addParam(0).addParam(10).execQuery() ;
		rows.debugPrint(); 
	}
	
	
}
