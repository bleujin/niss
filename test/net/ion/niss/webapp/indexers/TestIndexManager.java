package net.ion.niss.webapp.indexers;

import java.io.IOException;
import java.io.Reader;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;

import net.ion.framework.util.DateUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.common.FieldIndexingStrategy;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.common.MyField.MyFieldType;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.radon.client.StubServer;
import junit.framework.TestCase;

public class TestIndexManager extends TestCase {

//	private StubServer ss;
//	private REntry entry;
//
//	@Override
//	protected void setUp() throws Exception {
//		super.setUp();
//
//		this.ss = StubServer.create(IndexerWeb.class);
//		this.entry = REntry.test();
//		ss.treeContext().putAttribute(REntry.EntryName, entry);
//
//		if (! entry.indexManager().hasIndex("col1")){
//			StubHttpResponse response = ss.request("/indexers/col1").postParam("cid", "col1").post();
//			assertEquals("created col1", response.contentsString());
//		}
//	}
//
//	@Override
//	protected void tearDown() throws Exception {
//		ss.shutdown();
//		super.tearDown();
//	}
	
	
	
	
	
	
	
	public void testKeyword() throws Exception {
		Central c = CentralConfig.newRam().build() ;
		Indexer indexer = c.newIndexer() ;
		
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.newDocument().unknown("name", "0708").insert() ;
				return null;
			}
		}) ;

		// c.newSearcher().search("name:0708").debugPrint(); 

		c.newSearcher().search("0708").debugPrint("name"); 
		c.close(); 
	}
}
