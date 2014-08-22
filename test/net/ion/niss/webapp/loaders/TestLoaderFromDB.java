package net.ion.niss.webapp.loaders;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.loaders.ExceptionHandler;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.RDB;

import org.infinispan.util.concurrent.WithinThreadExecutor;

public class TestLoaderFromDB extends TestCase {

	public void testBean() throws Exception {
		final Writer writer = new OutputStreamWriter(System.out, "EUC-KR");
		final JsonWriter jwriter = new JsonWriter(writer);

		RDB.oracle("61.250.201.239:1521:qm10g", "bleujin", "redf").query("select * from tabs").handle(new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				try {
					jwriter.beginArray();

					while (rs.next()) {
						jwriter.beginObject();
						jwriter.name("table_name").value(rs.getString("table_name"));
						jwriter.endObject();
					}
					jwriter.endArray();
					jwriter.close();
				} catch (IOException e) {
					throw new SQLException(e);
				}
				return null;
			}
		});

	}

	public void testCreate() throws Exception {
		JScriptEngine app = JScriptEngine.create() ;
		
		InstantJavaScript script = app.createScript(IdString.create("sample_db"), "Sample From DB", JScriptEngine.class.getResourceAsStream("fromdb.txt")) ;
		
		Writer writer =  new OutputStreamWriter(System.out, "EUC-KR");
		script.runAsync(writer, ExceptionHandler.DEFAULT) ;
	}
}
