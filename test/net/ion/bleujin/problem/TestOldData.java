package net.ion.bleujin.problem;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import junit.framework.TestCase;

public class TestOldData extends TestCase{

	public void testRead() throws Exception {
		Craken craken = Craken.local() ;
		craken.createWorkspace("admin", WorkspaceConfigBuilder.oldDir("./resource/admin/old07")) ;
		
		ReadSession session = craken.login("admin") ;
	}
}
