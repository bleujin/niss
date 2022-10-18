package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.TransactionJobs;
import net.ion.niss.webapp.REntry;

public class TestCrakenLet extends TestCase {

	protected REntry re ; 
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		re = REntry.test() ;
		this.session = re.login() ;
		session.tranSync(TransactionJobs.dummy("bleujin", 10)) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		re.repository().shutdown();
		super.tearDown();
	}

	
	public void testRender() throws Exception {
		Engine engine = session.workspace().parseEngine();
		ReadNode find = session.pathBy("/bleujin") ;
		
		Debug.line(find.toMap()) ;
		
		String result = engine.transform(IOUtil.toStringWithClose(getClass().getResourceAsStream("craken.tpl")), MapUtil.<String, Object>create("self", find)) ;
		
		Debug.line(result);
	}
	
	public void testDirect() throws Exception {
		ReadNode found = session.pathBy("/bleujin");
		Engine engine = session.workspace().parseEngine();
		
		String result = engine.transform("${foreach self.children().stream().gte(dummy,3).lte(dummy,5).descending(dummy) child ,}${child}${end}", MapUtil.<String, Object>create("self", found)) ;
		Debug.line(result) ;
	}
}
