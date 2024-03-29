package net.ion.niss.webapp.misc;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import net.bleujin.rcraken.ReadSession;
import net.bleujin.searcher.common.FieldIndexingStrategy;
import net.bleujin.searcher.common.WriteDocument;
import net.bleujin.searcher.index.IndexSession;
import net.ion.framework.util.Debug;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.TestBaseIndexWeb;
import net.ion.niss.webapp.loaders.RDB;
import net.ion.nsearcher.index.file.FileEntry;
import net.ion.nsearcher.index.file.FileIndexBuilder;
import net.ion.nsearcher.index.file.FileIndexHandler;
import net.ion.nsearcher.index.file.FileIndexer;
import net.ion.nsearcher.index.rdb.FailHandler;
import net.ion.nsearcher.index.rdb.FileEntryFactory;
import net.ion.nsearcher.index.rdb.RDBIndexBuilder;
import net.ion.nsearcher.index.rdb.RDBIndexHandler;
import net.ion.nsearcher.index.rdb.RDBIndexer;

public class TestSampleScript2 extends TestBaseIndexWeb {

	private REntry rentry;
	private IndexManager imanager;
	private ReadSession rsession;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.rentry = ss.treeContext().getAttributeObject(REntry.EntryName, REntry.class) ;
		
		this.imanager = rentry.indexManager();
		this.rsession = rentry.login() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		rentry.close();
		super.tearDown();
	}
	
	public void testIndexFromFile() throws Exception {
		final String iid = "col1" ;

		FileIndexer findexer = FileIndexBuilder.create(imanager.index(iid))
					.baseDir(new File("./resource")).recursive(true)
					.extNames(new String[] { "docx", "pptx", "rtf", "txt", "xls", "pdf", "hwp" })
					.executors(new WithinThreadExecutor())
					.build();

		Future<List<Boolean>> future = findexer.index(new StandardAnalyzer(), new FileIndexHandler<Boolean>() {
			@Override
			public Boolean onSuccess(IndexSession isession, FileEntry fentry) throws IOException {
				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				
				Map<String, String> metaMap = fentry.meta() ;
				
				isession.newDocument(fentry.file().getCanonicalPath())
					.unknown("content", fentry.contentBuffer().toString())
					.updateVoid() ;
				return true;
			}

			@Override
			public Boolean onFail(IndexSession isession, File file, Exception ex) {
				Debug.debug(file, ex);
				return false;
			}
			
			private FieldIndexingStrategy createIndexStrategy(String iid) {
				return imanager.fieldIndexStrategy(rsession, iid) ;
			}
		});
		
		imanager.index(iid).newSearcher().search("클라우드").debugPrint(); 
	}
	
	public void testFromRDB() throws Exception {
		final String iid = "col1" ;
		
		RDBIndexer rindexer = RDBIndexBuilder.create(imanager.index(iid))
			.rdb(RDB.oracle("61.250.201.239:1521:qm10g", "bleujin", "redf").query("select * from tabs"))
			.executors(new WithinThreadExecutor()) //
			.build() ;
		
		Future<Integer> future = rindexer.index(new RDBIndexHandler<Integer>() {
			@Override
			public Integer onSuccess(IndexSession isession, RDB rdb, ResultSet rs) throws IOException, SQLException {
				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				int i = 0 ;
				while(rs.next()){
					isession.newDocument(rs.getString("table_name")).unknown("owner", "bleujin").updateVoid() ;
					if (i++ % 2999 == 0) isession.continueUnit() ;
				}
				return i;
			}

			@Override
			public Integer onFail(IndexSession isession, RDB rdb, Exception ex) {
				return 0;
			}
			
			private FieldIndexingStrategy createIndexStrategy(String iid) {
				return imanager.fieldIndexStrategy(rsession, iid) ;
			}
		});
		
		imanager.index(iid).newSearcher().search("").debugPrint(); 
	}
	
	
	
	
	
	public void testFromDBWithFileLoc() throws Exception {
		final String iid = "col1" ;
		
		RDBIndexer rindexer = RDBIndexBuilder.create(imanager.index(iid))
			.rdb(RDB.oracle("61.250.201.239:1521:qm10g", "bleujin", "redf").query("select table_name, './resource/dir_help.txt' fileloc from tabs"))
			.executors(new WithinThreadExecutor()) //
			.build() ;
		
		rindexer.index(new RDBIndexHandler<Integer>() {
			@Override
			public Integer onSuccess(IndexSession isession, RDB rdb, ResultSet rs) throws IOException, SQLException {
				isession.fieldIndexingStrategy(createIndexStrategy(iid)) ;
				int i = 0 ;
				
				FileEntryFactory fef = FileEntryFactory.create() ;
				
				while(rs.next()){
					WriteDocument wdoc = isession.newDocument(rs.getString("table_name")).unknown("owner", "bleujin") ;
					
					File file = new File(rs.getString("fileloc")) ;
					if (file.exists() && file.isFile() && file.canRead()){ // and check other
						FailHandler<FileEntry> nullHandler = new FailHandler<FileEntry>() {
							public FileEntry onFail(File file, Exception ex) {
								return null;
							}
						};
						FileEntry fentry = fef.makeEntry(file, nullHandler) ;
						if (fentry != null) {
							wdoc.keyword("file", file.getCanonicalPath()) ;
							wdoc.text("content", fentry.contentBuffer().toString()) ;
							wdoc.keyword("owner", "withfile") ;
						}
					}
					
					wdoc.updateVoid() ;
					if (i++ % 2999 == 0) isession.continueUnit() ;
				}
				return i;
			}

			@Override
			public Integer onFail(IndexSession isession, RDB rdb, Exception ex) {
				return 0;
			}
			
			private FieldIndexingStrategy createIndexStrategy(String iid) {
				return imanager.fieldIndexStrategy(rsession, iid) ;
			}
		});
		
		rentry.indexManager().index("col1").newSearcher().createRequest("owner:withfile").find().debugPrint();
		
	}
	
	
}
