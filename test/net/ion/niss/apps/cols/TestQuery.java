package net.ion.niss.apps.cols;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import junit.framework.TestCase;
import net.ion.framework.db.Page;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;
import net.ion.niss.apps.collection.FieldSchema;
import net.ion.niss.apps.old.IndexCollection;
import net.ion.niss.apps.old.IndexManager;
import net.ion.niss.webapp.collection.ResFns;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.radon.util.csv.CsvWriter;

public class TestQuery extends TestCase {

	private IndexManager ca;
	private IndexCollection ic;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.ca = IndexManager.create();
		if (!ca.hasCollection("document")) {
			ca.newCollection("document");
		}
		this.ic = ca.find("document");

	}

	@Override
	protected void tearDown() throws Exception {
		ca.shutdown();
		super.tearDown();
	}

	public void testIndexSample() throws Exception {
		InputStream input = TestDocument.class.getResourceAsStream("sample.json");
		JsonReader jreader = new JsonReader(new InputStreamReader(input));

		ic.index(FieldSchema.DEFAULT, jreader);

		jreader.close();
		ic.searcher().search("").debugPrint();
	}

	public void testQueryAll() throws Exception {
		ic.searcher().createRequest("*:*").skip(0).offset(4).sort("id desc").find().debugPrint();
	}

	public void testResultToJson() throws Exception {
		SearchResponse response = ic.searcher().createRequest("*:*").find();
		StringWriter swriter = new StringWriter();
		
		response.transformer(ResFns.createjsonWriterFn(response, MapUtil.EMPTY, swriter)) ;
		Debug.line(swriter);
	}

	
	public void testResultToCSV() throws Exception {
		SearchResponse response = ic.searcher().createRequest("*:*").find();
		StringWriter swriter = new StringWriter();
		
		response.transformer(ResFns.createCSVWriterFn(response, swriter)) ;
		Debug.line(swriter);
	}
	
	public void testPage() throws Exception {
		SearchResponse response = ic.searcher().createRequest("*:*").page(Page.create(2, 1)).find();
		response.debugPrint(); 
		Debug.line(response.totalCount());
	}
	
	
	

}
