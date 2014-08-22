package net.ion.niss.webapp.loaders;

import java.sql.SQLException;

import net.ion.framework.db.DBController;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.db.manager.DBManager;
import net.ion.framework.db.manager.OracleDBManager;
import net.ion.framework.db.procedure.IUserCommand;

public class RDB {

	private DBManager dbm;
	private String query;
	
	public RDB(DBManager dbm) {
		this.dbm = dbm ;
	}

	public static RDB oracle(String url, String userId, String userPwd) {
		OracleDBManager dbm = new OracleDBManager("jdbc:oracle:thin:@61.250.201.239:1521:qm10g", "bleujin", "redf") ;
		return new RDB(dbm) ;
	}

	public RDB query(String query) {
		this.query = query ;
		return this ;
	}

	public <T> T handle(ResultSetHandler<T> rhandler) throws SQLException {
		DBController dc = new DBController(dbm) ;
		try {
			dc.initSelf();
			IUserCommand cmd = dc.createUserCommand(query) ;
			T result = cmd.execHandlerQuery(rhandler) ;
			return result ;
		} finally {
			dc.destroySelf();
		}
		
	}

}
