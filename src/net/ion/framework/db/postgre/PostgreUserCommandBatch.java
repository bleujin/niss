package net.ion.framework.db.postgre;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import net.ion.framework.db.IDBController;
import net.ion.framework.db.procedure.MSSQLParamUtils;
import net.ion.framework.db.procedure.UserCommandBatch;
import net.ion.framework.util.Debug;

public class PostgreUserCommandBatch extends UserCommandBatch{

	public PostgreUserCommandBatch(IDBController dc, String strSQL) {
		super(dc, strSQL);
	}
	
	
	public int myUpdate(Connection conn) throws SQLException {
		int updateCount;
		try {
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(getProcSQL());
			pstmt.clearBatch();

			Object[][] values = getParamsAsArray(); // [a][b] a : rows, b : column
			for (int row = 0, rlast = values.length > 0 ? values[0].length : 0; row < rlast; row++) {
				List<Object> paramList = new ArrayList<Object>();
				for (int col = 0, clast = values.length; col < clast; col++) {
					Object obj = values[col][row];
					paramList.add(obj);
				}
				
				for (int i = 0; i < paramList.size(); i++) {
					final Object val = paramList.get(i);
					int type = getType(i) ;
					if (val == null) { 
						pstmt.setNull(i+1, type);
					} else 
						pstmt.setObject(i+1, val);
				}
				//MSSQLParamUtils.setCommandParam(pstmt, paramList, getTypes());
				pstmt.addBatch();
			}
			int[] count = pstmt.executeBatch();
			updateCount = count.length;

			pstmt.close();
			pstmt = null;

		} catch (SQLException ex) {
			throw ex;
		} finally {
			closeSilence(pstmt);
		}
		return updateCount;
	}
}
