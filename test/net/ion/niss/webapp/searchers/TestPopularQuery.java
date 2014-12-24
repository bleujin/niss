package net.ion.niss.webapp.searchers;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;
import junit.framework.TestCase;

public class TestPopularQuery extends TestCase {

	public void testThinking() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		ReadSession session = r.login("test");

		for (int i = 0; i < 10; i++) {
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession isession) throws Exception {
					isession.pathBy("/searchlogs/bleujin/20141214").increase("검색어" + RandomUtil.nextInt(3));
					return null;
				}
			});
		}

		ReadNode found = session.pathBy("/searchlogs/bleujin/20141214");
		for (PropertyId pid : found.keys()) {
			Debug.debug(pid, found.propertyId(pid).asLong(0));
		}

		r.shutdown();
	}

	public void testHasManyProperty() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		ReadSession session = r.login("test");

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession isession) throws Exception {
				WriteNode found = isession.pathBy("/searchlogs/bleujin/20141214");
				for (int i = 0; i < 10000; i++) {
					found.increase("검색어" + RandomUtil.nextInt(10000));
				}
				return null;
			}
		});

		ReadNode found = session.pathBy("/searchlogs/bleujin/20141214");
		for (PropertyId pid : found.keys()) {
			Debug.debug(pid, found.propertyId(pid).asLong(0));
		}

		r.shutdown();
	}
}
