package net.ion.niss.webapp.collection;

import net.ion.niss.apps.collection.IndexCollectionApp;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.nsearcher.search.Searcher;

public class TestIndex extends TestBaseWeb {

	
	public void testJsonUpdate() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/index.json").postParam("documents", "{id:'json', name:'bleujin', age:20}").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("1 indexed", response.contentsString());
		
		IndexCollectionApp ca = ss.treeContext().getAttributeObject(IndexCollectionApp.class.getSimpleName(), IndexCollectionApp.class) ;
		Searcher searcher = ca.find("col1").searcher() ;
		searcher.createRequest("id:json").find().debugPrint();
	}
	
	public void testJarrayUpdate() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/index.jarray").postParam("documents", "[{id:'jarray1', name:'jarray', age:20}, {id:'jarray2', name:'jarray', age:30}]").postParam("boost", "1.0").postParam("overwrite", "true").post() ;
		assertEquals("2 indexed", response.contentsString());

		IndexCollectionApp ca = ss.treeContext().getAttributeObject(IndexCollectionApp.class.getSimpleName(), IndexCollectionApp.class) ;
		Searcher searcher = ca.find("col1").searcher() ;
		searcher.createRequest("name:jarray").find().debugPrint();
	}
	
	public void testCsvUpdate() throws Exception {
		StubHttpResponse response = ss.request("/collections/col1/index.csv").postParam("documents", "id,name,age,title\ncsv1,csv,20,new\ncsv2,csv,30,new").postParam("boost", "2.0").postParam("overwrite", "true").post() ;
		assertEquals("2 indexed", response.contentsString());
		IndexCollectionApp ca = ss.treeContext().getAttributeObject(IndexCollectionApp.class.getSimpleName(), IndexCollectionApp.class) ;
		Searcher searcher = ca.find("col1").searcher() ;
		searcher.createRequest("name:csv").find().debugPrint();
		
	}
}
