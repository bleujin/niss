package net.ion.niss.webapp.searchers;

import junit.framework.TestCase;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.SearchControllerConfig;
import net.bleujin.searcher.search.SearchResponse;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.indexers.IndexJobs;

public class TestArgument extends TestCase {

	public void testReadDoc() throws Exception {
		SearchController c = SearchControllerConfig.newRam().build();

		c.index(IndexJobs.create("/bleujin", 10));
		SearchResponse response = c.newSearcher().search("");

		Engine engine = Engine.createDefaultEngine();
		String template = IOUtil.toStringWithClose(getClass().getResourceAsStream("tem.tpl"));

		String result = engine.transform(template, MapUtil.<Object> chainKeyMap().put("response", response).toMap());

		response.getDocument().get(0).asString("id");

		Debug.line(result);
		c.close();
	}
}
