package net.ion.niss.webapp.scripters;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectId;

public class TestRecent101 extends TestCase {

	
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
	
	public void testScheduleLog() throws Exception {
		session.tran(wsession -> {
			for (int i = 0; i < 240 ; i++) {
				wsession.pathBy("/scripts/abcd/" + (i % 101)).property("time", new ObjectId().toString()).merge();
			}
			return null;
		}) ;
		
		assertEquals(101, session.pathBy("/scripts/abcd").children().stream().count()) ;
		session.pathBy("/scripts/abcd").children().stream().ascending("time").debugPrint(); ;
	}
	
	public void testProperty() throws Exception {
		Debug.line(new ScheduleUtil().nextDate(-2), new ScheduleUtil().nextDate(-1), new ScheduleUtil().nextDate(0)) ;
	}
	
}
