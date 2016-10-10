package net.ion.framework.db.postgre;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import net.ion.framework.db.IDBController;
import net.ion.framework.db.procedure.UserProcedureBatch;
import net.ion.framework.util.StringUtil;


public class PostgreUserProcedureBatch extends UserProcedureBatch {
	private CallableStatement cstmt;

	private String stmtSQL;

	public PostgreUserProcedureBatch(IDBController dc, String procName) {
		super(dc, procName);
		this.stmtSQL = procName;
	}

	public int myUpdate(Connection conn) throws SQLException {
		int updateCount;
		try {
			cstmt = conn.prepareCall("{? = call " + getStmtSQL() + "}");
			cstmt.registerOutParameter(1, Types.OTHER);
			
			Object[][] values = getParamsAsArray(); // [a][b] a : rows, b : column
			
			for (int i = 0; i < values.length; i++) {
				cstmt.setArray(i+2, asArray(conn, values[i]));
			}
			if (values.length == 0){
				for (int i = 0; i < cstmt.getParameterMetaData().getParameterCount()-1 ; i++) {
					String typeName = cstmt.getParameterMetaData().getParameterTypeName(i+1) ; // int4[] cstmt.getParameterMetaData().getParameterType(2) 
					if (typeName.startsWith("int") || typeName.startsWith("_int")) {
						cstmt.setArray(i+1, asArray(conn, new Integer[0]));
					} else if (typeName.startsWith("void")) {
					} else {
						cstmt.setArray(i+1, asArray(conn, new Object[0]));
					}
				}
			}
			
			if (values.length > 0) {
				cstmt.execute();
			}
			updateCount = values.length;

			cstmt.close();
			cstmt = null;
		} catch (SQLException ex) {
			throw ex;
		} finally {
			closeSilence(cstmt);
		}
		return updateCount;
	}

	private Array asArray(Connection conn, Object[] val) throws SQLException{
		Class ctype = val.getClass().getComponentType() ;
		if (ctype == Integer.class || (val.length > 0 && val[0] instanceof Integer)){
			return conn.createArrayOf("integer", val) ;
		} else {
			return conn.createArrayOf("varchar", val) ;
		}
	}
	
	protected String getStmtSQL() {
		return StringUtil.replace(transNamedSQL(this.stmtSQL), "@", "$");
	}

	public Statement getStatement() {
		return cstmt;
	}

	
//	private void makeQueryStatement(Connection conn) throws SQLException {
//		cstmt = conn.prepareCall("{? = call " + getProcedureSQL() + "}", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
//		setMaxRows(cstmt, getMaxRows());
//		cstmt.setFetchSize(getFetchSize());
//
////		cstmt.setRowPrefetch(getFetchSize());
//		cstmt.registerOutParameter(1, Types.OTHER);
//		for (int i = 0; i < getParams().size(); i++) {
//			setQueryParam(i, getParams().get(i));
//		}
//	}
}
