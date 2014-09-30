package net.ion.niss.webapp.searchers;

import java.io.FileInputStream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.infinispan.util.concurrent.WithinThreadExecutor;

import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.loaders.ExceptionHandler;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.config.SearchConfig;
import net.ion.nsearcher.index.IndexJobs;
import net.ion.nsearcher.search.CompositeSearcher;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.Searcher;
import net.ion.nsearcher.search.filter.TermFilter;
import net.ion.nsearcher.search.processor.PostProcessor;
import junit.framework.TestCase;

public class TestScriptFilter extends TestCase {

	private Searcher searcher;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Central c1 = CentralConfig.newRam().build();
		Central c2 = CentralConfig.newRam().build();

		SearchConfig nconfig = SearchConfig.create(new WithinThreadExecutor(), SearchConstant.LuceneVersion, new StandardAnalyzer(SearchConstant.LuceneVersion), SearchConstant.ISALL_FIELD);
		this.searcher = CompositeSearcher.create(nconfig, ListUtil.toList(c1, c2));

		c1.newIndexer().index(IndexJobs.create("bleujin", 2));
		c2.newIndexer().index(IndexJobs.create("hero", 2));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testTermFilter() throws Exception {

		assertEquals(4, searcher.search("").size());

		TermFilter filter = new net.ion.nsearcher.search.filter.TermFilter("idx", "1");
		assertEquals(2, searcher.andFilter(filter).search("").size());
	}

	public void testPostListener() throws Exception {
		searcher.addPostListener(new PostProcessor() {
			@Override
			public void postNotify(SearchRequest req, SearchResponse res) {
				Debug.line(req, res);
			}
		});
	}

	public void testScriptFilter() throws Exception {
		InstantJavaScript script = JScriptEngine.create().createScript(IdString.create("filter"), "", getClass().getResourceAsStream("filter.script"));
		Searcher fsearcher = script.exec(new ResultHandler<Searcher>() {
			@Override
			public Searcher onSuccess(Object result, Object... args) {
				return (Searcher) result;
			}

			@Override
			public Searcher onFail(Exception ex, Object... args) {
				ex.printStackTrace();
				return (Searcher) args[0];
			}
		}, searcher);

		fsearcher.createRequest("").find().debugPrint();
	}
}