package net.ion.niss.webapp.scripters;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.ReadStream;
import net.bleujin.rcraken.WriteNode;
import net.bleujin.rcraken.extend.ChildQueryRequest;
import net.bleujin.rcraken.extend.ChildQueryResponse;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.SearchControllerConfig;
import net.bleujin.searcher.SearchRequestWrapper;
import net.bleujin.searcher.Searcher;
import net.bleujin.searcher.search.SearchRequest;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.indexers.IndexJobs;

public class TestRecommand extends TestCase{
	
	
	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = CrakenConfig.mapMemory().build().start() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
	public void testFind() throws Exception {
		session.tran(wsession -> {
			wsession.pathBy("/logs/1").property("query", "bleujin").merge();
			wsession.pathBy("/logs/2").property("query", "jin").merge() ;
			wsession.pathBy("/logs/3").property("query", "hero").merge() ;
			wsession.pathBy("/logs/4").property("query", "novision").merge() ;
			wsession.pathBy("/logs/5").property("query", "yucea").merge() ;
			wsession.pathBy("/logs/6").property("query", "mint").merge() ;
		}) ;
		
		session.pathBy("/logs").childQuery("query:no*").find().debugPrint();
	}

	
	public void testFindInArray() throws Exception {
		session.tran(wsession -> {
			wsession.pathBy("/logs/1").property("query", "bleujin", "hero").merge();
			wsession.pathBy("/logs/2").property("query", "jin").merge() ;
		}) ;
		
		ChildQueryRequest request = session.pathBy("/logs").childQuery("hero");
		request.find().debugPrint();
		
		String result = request.find().transformer(new com.google.common.base.Function<ChildQueryResponse, String>() {
			@Override
			public String apply(ChildQueryResponse cresponse) {
				ReadStream iter = cresponse.stream() ;
				List list = ListUtil.newList() ;
				for(ReadNode node : iter){
					list.add(node.property("query").asString()) ;
				}
				return StringUtil.join(list, ',');
			}
		}) ;
		
		Debug.line(result);
		
	}
	
	
	public void testQueryRewrite() throws Exception {
		SearchController central = SearchControllerConfig.newRam().build() ;
		central.index(IndexJobs.create("bleujin", 10));
		
		Searcher searcher = central.newSearcher() ;
		SearchRequestWrapper request = searcher.createRequest("bleujin name:hero") ;
		
		Query query = request.query();
		Debug.line(query);
		query.rewrite(central.search(session -> session).indexReader()) ;
		HashSet<Term> terms = new HashSet<Term>() ;
		query.visit(QueryVisitor.termCollector(terms));
		// query.extractTerms(terms);
		
		Debug.line(terms, terms.toArray(new Term[0])[0].text());
		
	}
	
	

	public void testDateRange() throws Exception {
		session.tran(wsession -> {
			for (int i = 0; i <= 20; i++) {
				WriteNode queryNode = wsession.pathBy("/logs/" + RandomUtil.nextRandomString(10)) ;
				for (int j = 1; j <= 31; j++) {
					queryNode.property("d" + j, RandomUtil.nextInt(20)).merge() ;
				}
			}
			return null;
		}) ;
		
		
		session.pathBy("/logs").children().stream().transform(new Function<Iterable<ReadNode>, List<String>>(){
			@Override
			public List<String> apply(Iterable<ReadNode> iter) {
				ScheduleUtil su = new ScheduleUtil() ;

				for(ReadNode node : iter){
					long sum = node.property("d" + su.nextDate(-2)).asLong() + node.property("d" + su.nextDate(-1)).asLong() +node.property("d" + su.nextDate(0)).asLong() ;
					node.fqn().name() ;
				}
				
				return null;
			}
		}) ;
	}
}
 