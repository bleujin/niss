package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.niss.webapp.REntry;

public class TestSort extends TestCase {

	public void testSortLong() throws Exception {
		REntry re = REntry.test() ;
		
		ReadSession session = re.login() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 10; i++) {
					wsession.pathBy("/bleujin/" + i).property("dummy", 1L * i) ;
				}
				return null;
			}
		}) ;
		
		session.pathBy("/bleujin").childQuery("").descending("dummy").find().debugPrint(); 
	}
}
