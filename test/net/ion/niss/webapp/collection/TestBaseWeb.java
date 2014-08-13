package net.ion.niss.webapp.collection;

import net.ion.niss.apps.collection.IndexCollectionApp;
import net.ion.niss.apps.collection.IndexCollection;
import net.ion.niss.webapp.collection.OldCollectionWeb;
import net.ion.niss.webapp.loader.LoaderWeb;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.radon.client.StubServer;
import junit.framework.TestCase;

public class TestBaseWeb extends TestCase{

	protected StubServer ss;
	protected IndexCollectionApp ca;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(OldCollectionWeb.class, LoaderWeb.class) ;
		this.ca = IndexCollectionApp.test();
		ss.treeContext().putAttribute(IndexCollectionApp.class.getSimpleName(), ca) ;
		
		IndexCollection col1 = ca.newCollection("col1") ;
		col1.indexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				WriteDocument wdoc = isession.newDocument("query").keyword("name", "bleujin").number("age", 20) ;
				isession.updateDocument(wdoc) ;
				return null;
			}
		}) ;
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		ss.shutdown(); 
		super.tearDown();
	}
	
}
