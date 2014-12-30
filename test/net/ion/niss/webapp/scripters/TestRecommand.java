package net.ion.niss.webapp.scripters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJobs;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

import com.google.common.base.Function;

public class TestRecommand extends TestCase{
	
	
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
	public void testFind() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/logs/1").property("query", "bleujin") ;
				wsession.pathBy("/logs/2").property("query", "jin") ;
				wsession.pathBy("/logs/3").property("query", "hero") ;
				wsession.pathBy("/logs/4").property("query", "novision") ;
				wsession.pathBy("/logs/5").property("query", "yucea") ;
				wsession.pathBy("/logs/6").property("query", "mint") ;
				return null;
			}
		}) ;
		
		session.pathBy("/logs").childQuery("query:no*").find().debugPrint();
	}

	
	public void testFindInArray() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/logs/1").append("query", "bleujin", "hero") ;
				wsession.pathBy("/logs/2").property("query", "jin") ;
				return null ;
			}
		}) ;
		
		ChildQueryRequest request = session.pathBy("/logs").childQuery("hero");
		request.find().debugPrint();
		
		String result = request.find().transformer(new Function<ChildQueryResponse, String>() {
			@Override
			public String apply(ChildQueryResponse cresponse) {
				IteratorList<ReadNode> iter = cresponse.iterator() ;
				List list = ListUtil.newList() ;
				while(iter.hasNext()){
					list.add(iter.next().property("query").asString()) ;
				}
				return StringUtil.join(list, ',');
			}
		}) ;
		
		Debug.line(result);
		
	}
	
	
	public void testQuerrRewrite() throws Exception {
		Central central = CentralConfig.newRam().build() ;
		central.newIndexer().index(IndexJobs.create("bleujin", 10));
		
		Searcher searcher = central.newSearcher() ;
		SearchRequest request = searcher.createRequest("bleujin name:hero") ;
		
		Query query = request.query();
		Debug.line(query);
		query.rewrite(central.newReader().getIndexReader()) ;
		HashSet<Term> terms = new HashSet<Term>() ;
		query.extractTerms(terms);
		
		Debug.line(terms, terms.toArray(new Term[0])[0].text());
		
	}
	
	

	public void testDateRange() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i <= 20; i++) {
					WriteNode queryNode = wsession.pathBy("/logs/" + RandomUtil.nextRandomString(10)) ;
					for (int j = 1; j <= 31; j++) {
						queryNode.property("d" + j, RandomUtil.nextInt(20)) ;
					}
				}
				return null;
			}
		}) ;
		
		
		session.pathBy("/logs").children().transform(new Function<Iterator<ReadNode>, List<String>>(){
			@Override
			public List<String> apply(Iterator<ReadNode> iter) {
				ScheduleUtil su = new ScheduleUtil() ;
				
				
				while(iter.hasNext()){
					ReadNode node = iter.next() ;
					long sum = node.property("d" + su.nextDate(-2)).asLong(0) + node.property("d" + su.nextDate(-1)).asLong(0) +node.property("d" + su.nextDate(0)).asLong(0) ;
					node.fqn().name() ;
				}
				
				return null;
			}
		}) ;
	}
}
 