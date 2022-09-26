package net.ion.niss.webapp.searchers;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import junit.framework.TestCase;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.SearchControllerConfig;
import net.bleujin.searcher.Searcher;
import net.bleujin.searcher.search.SearchRequest;
import net.bleujin.searcher.search.SearchResponse;
import net.bleujin.searcher.search.processor.PostProcessor;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.indexers.IndexJobs;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;

public class TestScriptFilter extends TestCase {

	private Searcher searcher;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		SearchController c1 = SearchControllerConfig.newRam().build();
		SearchController c2 = SearchControllerConfig.newRam().build();

		StandardAnalyzer anal = new StandardAnalyzer();

		this.searcher = c2.newSearcher(c2) ;
		this.searcher.sconfig().queryAnalyzer(anal) ;

		c1.index(IndexJobs.create("bleujin", 2));
		c2.index(IndexJobs.create("hero", 2));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testTermFilter() throws Exception {

		assertEquals(4, searcher.search("").size());

		TermQuery filter = new TermQuery(new Term("idx", "1"));
		assertEquals(2, searcher.createRequest("").filter(filter).find().size());
	}

	public void testPostListener() throws Exception {
		searcher.addPostListener(new PostProcessor() {
			@Override
			public void process(SearchRequest req, SearchResponse res) {
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
