package net.ion.niss.webapp.scripters;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.framework.util.Debug;

import org.bson.types.ObjectId;

public class TestRecent101 extends TestCase {

	
	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
	public void testScheduleLog() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 240 ; i++) {
					wsession.pathBy("/scripts/abcd/" + (i % 101)).property("time", new ObjectId().toString()) ;
				}
				return null;
			}
		}) ;
		
		assertEquals(101, session.pathBy("/scripts/abcd").children().count()) ;
		session.pathBy("/scripts/abcd").children().ascending("time").debugPrint(); ;
	}
	
	public void testProperty() throws Exception {
		Debug.line(new ScheduleUtil().nextDate(-2), new ScheduleUtil().nextDate(-1), new ScheduleUtil().nextDate(0)) ;
	}
	
}
