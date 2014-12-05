package net.ion.nsearcher.index.file;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexSession;
import net.sf.jsqlparser.statement.insert.Insert;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.infinispan.util.concurrent.WithinThreadExecutor;

public class TestIndexFromFile extends TestCase {

	public void testInterface() throws Exception {
		Central cen = CentralConfig.newRam().build();

		FileIndexer findexer = FileIndexBuilder.create(cen)
					.baseDir(new File("./resource")).recursive(true)
					.extNames(new String[] { "docx", "pptx", "rtf", "txt", "xls", "pdf", "hwp" })
					.sizeFilter(10 * 1024, false) // less 10K
					// .prefixFilter("sample")
					// .ageOverFilter(new Date().getTime() - (86400L * 1000), false) // recent 1 day
					.executors(new WithinThreadExecutor())
					.build();

		Future<List<Boolean>> future = findexer.index(new StandardAnalyzer(SearchConstant.LuceneVersion), new FileIndexHandler<Boolean>() {
			@Override
			public Boolean onSuccess(IndexSession isession, FileEntry fentry) throws IOException {
				Debug.debug(fentry.file());
				isession.newDocument(fentry.file().getCanonicalPath()).text("content", fentry.contentBuffer().toString()).updateVoid() ;
				return true;
			}

			@Override
			public Boolean onFail(IndexSession isession, File file, Exception ex) {
				Debug.debug(file, ex);
				return false;
			}
		});
		
		cen.newSearcher().search("클라우드").debugPrint(); 

	}
}

