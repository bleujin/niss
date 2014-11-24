package net.ion.niss.webapp.misc;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.REntry;

public class TestCrakenLet extends TestCase {

	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		REntry re = REntry.test() ;
		this.session = re.login() ;
		session.tranSync(TransactionJobs.dummy("/bleujin", 10)) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown() ;
		super.tearDown();
	}

	
	public void testRender() throws Exception {
		Engine engine = session.workspace().parseEngine();
		ReadNode find = session.ghostBy("/") ;
		String result = engine.transform(IOUtil.toStringWithClose(getClass().getResourceAsStream("craken.tpl")), MapUtil.<String, Object>create("self", find)) ;
		
		Debug.line(result);
	}
}
