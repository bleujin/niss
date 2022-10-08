package net.ion.niss.webapp.dscripts;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.db.bean.handlers.MapListHandler;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;

public class JsonStringHandler implements ResultSetHandler<JsonObject> {
	private static final long serialVersionUID = 6041693596003912102L;

	public JsonObject handle(ResultSet rs) throws SQLException {
		ResultSetMetaData meta = rs.getMetaData();

		List<String> types = new ArrayList<String>();
		List<String> cols = new ArrayList<String>();
		int columnCount = meta.getColumnCount();

		for (int i = 0; i < columnCount; i++) {
			int column = i + 1;
			// if(! wantToView(meta, column)) continue ;
			types.add(meta.getColumnTypeName(column));
			cols.add(meta.getColumnName(column).toLowerCase());
		}

		List<Map<String, ? extends Object>> list = new MapListHandler().handle(rs);
		

		JsonObject body = new JsonObject();

		body.add("type", JsonParser.fromList(types));
		body.add("header", JsonParser.fromList(cols));
		body.add("rows", JsonParser.fromList(list));

		JsonObject result = new JsonObject();
		result.add("node", body);

		return result;
	}

}