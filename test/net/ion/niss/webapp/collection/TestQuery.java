package net.ion.niss.webapp.collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.niss.apps.collection.CollectionApp;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

import org.jboss.resteasy.util.HttpHeaderNames;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TestQuery extends TestCase {

	public void testJsonQuery() throws Exception {
		StubServer ss = StubServer.create(CollectionWeb.class) ;
		ss.treeContext().putAttribute(CollectionApp.class.getSimpleName(), CollectionApp.create()) ;
		StubHttpResponse response = ss.request("/collections/col1/query.json?query=*%3A*&key1=val&key2=ddd").get() ; // indent=true&
		
		assertEquals("application/json", response.header(HttpHeaderNames.CONTENT_TYPE)) ;
		Debug.line(response.contentsString()) ;
	}
	
	public void testXmlQuery() throws Exception {
		StubServer ss = StubServer.create(CollectionWeb.class) ;
		ss.treeContext().putAttribute(CollectionApp.class.getSimpleName(), CollectionApp.create()) ;
		
		StubHttpResponse response = ss.request("/collections/col1/query.xml?query=*%3A*&key1=val&key2=ddd").get() ; // indent=true&
		Debug.line(response.header(HttpHeaderNames.CONTENT_TYPE)) ;
		Debug.line(response.contentsString()) ;
	}

	public void testCsvQuery() throws Exception {
		StubServer ss = StubServer.create(CollectionWeb.class) ;
		ss.treeContext().putAttribute(CollectionApp.class.getSimpleName(), CollectionApp.create()) ;
		
		StubHttpResponse response = ss.request("/collections/col1/query.csv?query=*%3A*&key1=val&key2=ddd").get() ; // indent=true&
		Debug.line(response.header(HttpHeaderNames.CONTENT_TYPE)) ;
		Debug.line(response.contentsString()) ;
	}

	
	
	public void testPrettyXML() throws Exception {
		// Source xmlInput = new StreamSource(new StringReader("<a><b><c/><d>text D</d><e value='0'/></b></a>"));

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("company");
		doc.appendChild(rootElement);
 
		// staff elements
		Element staff = doc.createElement("Staff");
		rootElement.appendChild(staff);
 
		// set attribute to staff element
		Attr attr = doc.createAttribute("id");
		attr.setValue("1");
		staff.setAttributeNode(attr);
 
		// firstname elements
		Element firstname = doc.createElement("firstname");
		firstname.appendChild(doc.createTextNode("yong"));
		staff.appendChild(firstname);
 
		// lastname elements
		Element lastname = doc.createElement("lastname");
		lastname.appendChild(doc.createTextNode("mook kim"));
		staff.appendChild(lastname);
 
		// nickname elements
		Element nickname = doc.createElement("nickname");
		nickname.appendChild(doc.createTextNode("mkyong"));
		staff.appendChild(nickname);
 
		// salary elements
		Element salary = doc.createElement("salary");
		salary.appendChild(doc.createTextNode("100000"));
		staff.appendChild(salary);
		
		DOMSource source = new DOMSource(doc);
		StreamResult xmlOutput = new StreamResult(System.out);

		// Configure transformer
		Transformer transformer = SAXTransformerFactory.newInstance().newTransformer(); // An identity transformer
		// transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "testing.dtd");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2") ;
		transformer.transform(source, xmlOutput);
	}
}

