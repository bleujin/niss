package net.ion.niss.webapp.scripters;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.RandomUtil;
import junit.framework.TestCase;

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
 