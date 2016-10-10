package net.ion.framework.db.postgre;

import java.io.IOException;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import net.ion.framework.db.IDBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.db.procedure.UserProcedure;
import net.ion.framework.util.IOUtil;

import org.apache.commons.lang.ArrayUtils;

public class PostgreUserProcedure extends UserProcedure {

	protected CallableStatement cstmt;

	public PostgreUserProcedure(IDBController dc, String procSQL) {
		super(dc, procSQL);
	}

	public Statement getStatement() {
		return this.cstmt;
	}

	public Rows myQuery(Connection conn) throws SQLException {
		Rows result = null;
		ResultSet rs = null;

		try {
			conn.setAutoCommit(false);
			makeQueryStatement(conn);
			cstmt.execute();
			rs = (ResultSet) cstmt.getObject(1);
			result = super.populate(rs);
		} catch (SQLException ex) {
			throw new SQLException(getExceptionMessage(ex, this), ex.getSQLState(), ex.getErrorCode());
		} finally {
			closeSilence(rs, cstmt, conn);
		}

		return result;
	}

	private void makeQueryStatement(Connection conn) throws SQLException {
		cstmt = conn.prepareCall("{? = call " + getProcedureSQL() + "}", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		setMaxRows(cstmt, getMaxRows());
		cstmt.setFetchSize(getFetchSize());

//		cstmt.setRowPrefetch(getFetchSize());
		cstmt.registerOutParameter(1, Types.OTHER);
		for (int i = 0; i < getParams().size(); i++) {
			setQueryParam(i, getParams().get(i));
		}
	}

	protected String getProcedureSQL() {
		String procSQL = getProcSQL().replace('@', '$');
		if (! procSQL.endsWith(")")) procSQL = procSQL.trim() + "()" ; 
		return procSQL;
	}

	public int myUpdate(Connection conn) throws SQLException {
		int updateRow = 0;
		try {

			cstmt = conn.prepareCall("{? = call " + getProcedureSQL() + "}");
			int[] types = ArrayUtils.toPrimitive((Integer[]) getTypes().toArray(new Integer[0]));
			for (int i = 0; i < getParams().size(); i++) {
				int mediateLoc = i + 1; // return + start zero...
				Object obj = getParams().get(i);
				int type = types[i];
				if (isNull(i)) {
					cstmt.setNull(mediateLoc + 1, getType(i));
				} else if (Types.CLOB == type) {
					if (obj instanceof Reader){
						cstmt.setString(mediateLoc + 1, IOUtil.toStringWithClose((Reader)obj));
					} else 
						cstmt.setString(mediateLoc + 1, obj.toString());
				} else if (Types.BLOB == type){
					throw new UnsupportedOperationException("call bleujin") ;
				} else {
					cstmt.setObject(mediateLoc + 1, obj);
				}
			}
			
			cstmt.registerOutParameter(1, Types.INTEGER);
			cstmt.executeUpdate();
			updateRow = cstmt.getInt(1);
		} catch (IOException ex) {
			throw new SQLException(getExceptionMessage(ex, this));
		} catch (SQLException ex) {
			throw new SQLException(getExceptionMessage(ex, this), ex.getSQLState(), ex.getErrorCode());
		} finally {
			closeSilence(cstmt);
		}
		return updateRow;
	}

	@Override
	public Object myHandlerQuery(Connection conn, ResultSetHandler handler) throws SQLException {
		ResultSet rs = null;
		try {
			conn.setAutoCommit(false);
			makeQueryStatement(conn);
			cstmt.execute();
			rs = (ResultSet) cstmt.getObject(1);
			return handler.handle(rs);
		} finally {
			closeSilence(rs, cstmt, conn);
		}
	}

	private void setQueryParam(int i, Object obj) throws SQLException {
		int mediateLoc = i + 2; // return + start zero...

		if (isNull(i)) {
			cstmt.setNull(mediateLoc, getType(i));
			// modifyps.setNull();
		} else {
			cstmt.setObject(mediateLoc, obj);
		}
	}
	

}