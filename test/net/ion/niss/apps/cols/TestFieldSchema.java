package net.ion.niss.apps.cols;

import junit.framework.TestCase;
import net.ion.crawler.util.StringUtil;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.ObjectUtil;
import net.ion.niss.apps.CollectionApp;
import net.ion.niss.apps.FieldSchema;
import net.ion.niss.apps.IndexCollection;
import net.ion.nsearcher.common.MyField;

public class TestFieldSchema extends TestCase {

	private CollectionApp ca;
	private IndexCollection ic;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		this.ca = CollectionApp.create() ;
		if (! ca.hasCollection("col1")){
			ca.newCollection("col1") ;
		}
		this.ic = ca.find("col1") ;
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		ca.shutdown() ;
		super.tearDown();
	}
	
	
	
	public void testFieldStrategy() throws Exception {
		JsonObject jo = JsonObject.create().put("id", "fstrategy_test").put("name", "gm200-a").put("age", 20) ;

		FieldSchema keywordSchema = new FieldSchema() {
			@Override
			public MyField toMyField(String key, JsonPrimitive value) {
				return MyField.text(key, ObjectUtil.toString(value.getValue()));
			}
		};
		
		
		ic.index(keywordSchema, jo) ;
		ic.searcher().search("gm200").debugPrint(); 
	}
	
	
	
}
