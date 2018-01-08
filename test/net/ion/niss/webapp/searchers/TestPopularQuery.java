package net.ion.niss.webapp.searchers;

import junit.framework.TestCase;
import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.WriteNode;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

public class TestPopularQuery extends TestCase {

	public void testThinking() throws Exception {
		Craken r = CrakenConfig.mapMemory().build().start();
		ReadSession session = r.login("test");

		for (int i = 0; i < 10; i++) {
			session.tran(isession -> {
				isession.pathBy("/searchlogs/bleujin/20141214").increase("검색어" + RandomUtil.nextInt(3)).merge();
			});
		}

		ReadNode found = session.pathBy("/searchlogs/bleujin/20141214");
		for (String pid : found.keys()) {
			Debug.debug(pid, found.property(pid).asLong());
		}

		r.shutdown();
	}

	public void testHasManyProperty() throws Exception {
		Craken r = CrakenConfig.mapMemory().build().start();
		ReadSession session = r.login("test");

		session.tran(isession -> {
			WriteNode found = isession.pathBy("/searchlogs/bleujin/20141214");
			for (int i = 0; i < 5000; i++) {
				found.increase("검색어" + RandomUtil.nextInt(10000)).merge();
			}
			return null;
		});

		ReadNode found = session.pathBy("/searchlogs/bleujin/20141214");
		for (String pid : found.keys()) {
			Debug.debug(pid, found.property(pid).asLong());
		}

		r.shutdown();
	}
}
