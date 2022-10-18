package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;

public class TestDataWeb extends TestCase{

	private REntry re;
	private ReadSession rsession;

	@Override
	protected void setUp() throws Exception {
		this.re = REntry.create() ;
		this.rsession = re.login("datas") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	
	public void testHasRef() throws Exception {
		rsession.pathBy("/boards").debugPrint() ;
		Debug.line(rsession.pathBy("/boardsabddccee").exist()) ;
	}
}
