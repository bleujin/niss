package net.ion.niss.webapp.datas;

import java.io.File;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.db.CrakenScriptManager;
import net.ion.framework.db.DBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.procedure.IUserProcedureBatch;
import net.ion.framework.util.Debug;

public class TestDScript extends TestCase {

	
	private DBController dc;
	private Craken c ;

	protected void setUp() throws Exception {
		c = CrakenConfig.mapMemory().build() ;
		c.start() ;
		ReadSession rsession = c.login("testworkspace") ;
		
		CrakenScriptManager dbm = CrakenScriptManager.create(rsession, Executors.newScheduledThreadPool(1), new File("./test/net/bleujin/rcraken/db")) ;
		this.dc = new DBController("craken", dbm);
		dc.initSelf() ;
	}
	
	protected void tearDown() throws Exception {
		dc.destroySelf();
		c.shutdown(); 
	}
	
	
	public void testCreateUserProcedure() throws Exception {
		int result = dc.createUserProcedure("afield@createWith(?,?)").addParam("rday").addParam("registerDay").execUpdate() ;
		Debug.line(result);
		
		Rows rows = dc.createUserProcedure("afield@listBy(?,?)").addParam(0).addParam(2).execQuery() ;
		rows.debugPrint(); 
		rows.first() ;
		assertEquals("rday", rows.getString("afieldId"));
	}
	
	
	
	public void xtestCreateUserProcedureBatch() throws Exception {
		IUserProcedureBatch bat = dc.createUserProcedureBatch("afield@batchWith(?,?)") ;
		bat.addBatchParam(0, "rday") ;
		bat.addBatchParam(1, "cday") ;

		bat.addBatchParam(0, "registerday");
		bat.addBatchParam(1, "createday");
		int result = bat.execUpdate() ;
		
		assertEquals(2, result);
		Rows rows = dc.createUserProcedure("afield@listBy(?,?)").addParam(0).addParam(5).execQuery() ;
		rows.debugPrint();
		
		dc.createUserProcedure("afield@removeAllWith()").execUpdate() ;
	}
	
}
