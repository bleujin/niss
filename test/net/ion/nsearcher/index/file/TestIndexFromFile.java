package net.ion.nsearcher.index.file;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import junit.framework.TestCase;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.SearchControllerConfig;
import net.bleujin.searcher.common.SearchConstant;
import net.bleujin.searcher.index.IndexSession;
import net.ion.framework.util.Debug;
import net.ion.framework.util.WithinThreadExecutor;

public class TestIndexFromFile extends TestCase {

	public void testInterface() throws Exception {
		SearchController cen = SearchControllerConfig.newRam().build();

		FileIndexer findexer = FileIndexBuilder.create(cen)
					.baseDir(new File("./resource")).recursive(true)
					.extNames(new String[] { "docx", "pptx", "rtf", "txt", "xls", "pdf", "hwp" })
					.sizeFilter(10 * 1024, false) // less 10K
					// .prefixFilter("sample")
					// .ageOverFilter(new Date().getTime() - (86400L * 1000), false) // recent 1 day
					.executors(new WithinThreadExecutor())
					.build();

		Future<List<Boolean>> future = findexer.index(new StandardAnalyzer(), new FileIndexHandler<Boolean>() {
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

