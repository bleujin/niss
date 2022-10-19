package net.ion.niss.webapp.indexers;

import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestBaseIndexWeb extends TestCase{

	protected StubServer ss;
	protected REntry entry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ss = StubServer.create(IndexerWeb.class);
		this.entry = REntry.testStup();
		ss.treeContext().putAttribute(REntry.EntryName, entry);

		final JScriptEngine jsentry = ss.treeContext().putAttribute(JScriptEngine.EntryName, JScriptEngine.create("./resource/loader/lib", Executors.newSingleThreadScheduledExecutor(), true));
		jsentry.executorService(Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("jscript-thread-%d")));

		
		if (! entry.indexManager().hasIndex("col1")){
			StubHttpResponse response = ss.request("/indexers/col1").post();
			assertEquals("created col1", response.contentsString());
		}
	}

	@Override
	protected void tearDown() throws Exception {
		ss.shutdown();
		super.tearDown();
	}
}
