package net.ion.niss.apps.loader;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.IOUtil;

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

	public void testFromDB() throws Exception {
		String script = IOUtil.toStringWithClose(getClass().getResourceAsStream("fromdb.script"));
		Writer writer = new OutputStreamWriter(System.out, "EUC-KR");
		
		Loader.fromScript(script).executor(new WithinThreadExecutor()).run(writer) ;
	}
}
