package net.ion.niss.webapp.misc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.convert.Functions;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.Searcher;
import net.bleujin.searcher.common.FieldIndexingStrategy;
import net.bleujin.searcher.common.WriteDocument;
import net.bleujin.searcher.index.IndexJob;
import net.bleujin.searcher.index.IndexSession;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.EventSourceEntry;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.TestBaseIndexWeb;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.RDB;
import net.ion.niss.webapp.scripters.ScriptWeb;

public class TestSampleScript extends TestBaseIndexWeb {

	public void testIndexFromScript() throws Exception {
		final REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;

		final String iid = "col1" ;
		final IndexManager imanager = rentry.indexManager();
		final SearchController central = imanager.index(iid);
		final ReadSession rsession = rentry.login() ;

		Callable<Void> callable = new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				RDB.oracle("61.250.201.239:1521:qm10g", "bleujin", "redf").query("select * from tabs").handle(new ResultSetHandler<Void>() {
					public Void handle(final ResultSet rs) throws SQLException {
						central.indexTran(new IndexJob<Void>() {
							@Override
							public Void handle(IndexSession isession) throws Exception {
								
								isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
								while(rs.next()){
									WriteDocument wdoc = isession.newDocument(rs.getString("table_name")) ;
									wdoc.keyword("owner", "hero").updateVoid() ;
								}
								rs.close(); 
								return null;
							}

							private FieldIndexingStrategy createIndexStrategy(String iid) {
								return imanager.fieldIndexStrategy(rsession, iid) ;
							}
						}) ;
						return null;
					}
				}) ;
				return null;
			}
		} ;
		
		final JScriptEngine jengine = ss.treeContext().getAttributeObject(JScriptEngine.EntryName, JScriptEngine.class) ;
		
		ExecutorService es = jengine.executor()  ;
		Future<Void> future = es.submit(callable) ;
		
		future.get() ;
		Searcher searcher = imanager.index(iid).newSearcher() ;
		searcher.createRequest("owner:hero").find().debugPrint(); 
		
		runScript(new FileInputStream("./resource/script/index_from_db.script")) ;
	}
	
	public void testRunIndexFromDB() throws Exception {
		runScript(new FileInputStream("./resource/script/index_from_db.script")) ;
	}

	
	public void testRunIndexFromDBWithFile() throws Exception {
		runScript(new FileInputStream("./resource/script/index_from_db_with_file.script")) ;
	}

	
	public void testRunIndexFromFile() throws Exception {
		runScript(new FileInputStream("./resource/script/index_from_file.script")) ;
		entry.indexManager().index("col1").newSearcher().createRequest("").find().debugPrint(); 
	}

	
	public void testReadCraken() throws Exception {
		final REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		ReadSession session = rentry.login() ;
		
		JsonObject json = session.pathBy("/indexers").children().stream().transform(Functions.CHILDLIST);
		Debug.line(json);
		
		runScript(new FileInputStream("./resource/script/read_from_craken.script"));
	}
	
	public void testWriteCraken() throws Exception {
		final REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		ReadSession session = rentry.login() ;
		final MultivaluedMap<String, String> param = new MultivaluedMapImpl<String, String>() ;
		param.putSingle("name", "bleujin");
		
		String name = session.tranSync(wsession -> {
			wsession.pathBy("/mydata").property("name", param.getFirst("name")).merge();
			return param.getFirst("name");
		}) ;
		
		Debug.line(name);
		
		runScript(new FileInputStream("./resource/script/write_to_craken.script"));
	}
	
	public void runScript(InputStream input) throws Exception {
		final REntry rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		final JScriptEngine jengine = ss.treeContext().getAttributeObject(JScriptEngine.EntryName, JScriptEngine.class) ;
		EventSourceEntry esentry = EventSourceEntry.create() ;
		ScriptWeb sweb = new ScriptWeb(rentry, jengine, esentry) ;
		
		Response response = sweb.runTestScript("test", new MultivaluedMapImpl<String, String>(), IOUtil.toStringWithClose(input)) ;
		Debug.line(response.getEntity()) ;
	}
	
	
	
	public void testBackupAdmin() throws Exception {
		ReadSession session = entry.login() ;
		
		Writer writer = new StringWriter() ;
		final JsonWriter jwriter = new JsonWriter(writer) ;
		
		session.root().transformer(new Function<ReadNode, Void>(){
			@Override
			public Void apply(ReadNode target) {
				target.walkBreadth(true, 10).stream().transform(new Function<Iterable<ReadNode>, Void>() {
					@Override
					public Void apply(Iterable<ReadNode> decent) {
						try {
							jwriter.beginObject() ;
							for(ReadNode node : decent){
								jwriter.jsonElement(node.fqn().toString(), node.toJson()) ;
							}
							jwriter.endObject() ;
							
						} catch (IOException e) {
							e.printStackTrace();
						}
						return null;
					}
				}) ;
				
				return null;
			}
		}) ;
		
		// Debug.line(writer.toString());
	}

	
	public void testRemoveIndex() throws Exception {
		SearchController central = entry.indexManager().index("col1") ;
		central.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.deleteTerm(new Term("name", "bleujin")) ;
				isession.deleteAll() ;
				isession.deleteById("bleujin") ;
				isession.deleteQuery(new WildcardQuery(new Term("name", "bleujin*"))) ;
				
				return null;
			}
		}) ;
	}
	
	
	public void testPlug() throws Exception {
		Debug.line(URLEncoder.encode("+")) ;
		
		
	}
}
