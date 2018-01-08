package net.ion.niss.webapp.common;

import java.io.FileInputStream;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import junit.framework.TestCase;
import net.bleujin.rcraken.script.StringInputStream;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.Webapp;

public class TestMessageEntity extends TestCase {

	public void testXMLUse() throws Exception {
		XMLReader xreader = XMLReaderFactory.createXMLReader();
		String xmlString = "<persons>hm.<person name='bleujin'>hello bleujin</person></persons>";
		InputSource input = new InputSource(new StringInputStream(xmlString));
		ToJsonHandler handler = new ToJsonHandler();
		xreader.setContentHandler(handler);
		xreader.parse(input);
		
		Debug.line(handler.root());
	}

	public void testFromFile() throws Exception {
		XMLReader xreader = XMLReaderFactory.createXMLReader();
		String xmlString = IOUtil.toStringWithClose(getClass().getResourceAsStream("messages.test.xml")) ;
		InputSource input = new InputSource(new StringInputStream(xmlString));

		ToJsonHandler handler = new ToJsonHandler();
		xreader.setContentHandler(handler);
		xreader.parse(input);
		
		Debug.line(handler.root());
		
		assertEquals("Create Loader", handler.root().asString("buttons.loaders.create")) ;
		assertEquals("buttons.loaders.create.unknown", handler.root().asString("buttons.loaders.create.unknown")) ;
		assertEquals("buttons.notfound.path", handler.root().asString("buttons.notfound.path")) ;

		assertEquals("hello bleujin", handler.root("param").asString("buttons.loaders.create", "bleujin")) ;
		assertEquals("hello bleujin", handler.root("param2").asString("buttons.loaders.create", "hello", "bleujin")) ;
		
		assertEquals("Menu Loader", handler.root().asString("menus.loaders")) ;
		assertEquals("Sub Menu", handler.root().asString("menus.loaders.sub")) ;
		assertEquals("Sub Menu KO", handler.root("ko").asString("menus.loaders.sub")) ;

	
		assertEquals("Menu Loader", handler.root("unknown").asString("menus.loaders")) ;
		
		
	}
	
	public void testAppliedLocale() throws Exception {
		XMLReader xreader = XMLReaderFactory.createXMLReader();
		String xmlString = IOUtil.toStringWithClose(getClass().getResourceAsStream("messages.test.xml")) ;
		InputSource input = new InputSource(new StringInputStream(xmlString));

		ToJsonHandler handler = new ToJsonHandler();
		xreader.setContentHandler(handler);
		xreader.parse(input);
		
		assertEquals("Create Loader", handler.root().asString("buttons.loaders.create")) ;
		
		assertEquals("cl", handler.root("en").asString("buttons.loaders.create")) ;
		assertEquals("로더 만들기", handler.root("ko").asString("buttons.loaders.create")) ;
	}
	
	
	public void testAsString() throws Exception {
		XMLReader xreader = XMLReaderFactory.createXMLReader();
		String xmlString = IOUtil.toStringWithClose(new FileInputStream(Webapp.MESSAGE_RESOURCE_FILE)) ;
		InputSource input = new InputSource(new StringInputStream(xmlString));

		ToJsonHandler handler = new ToJsonHandler();
		xreader.setContentHandler(handler);
		xreader.parse(input);
		
		MessageEntity m = handler.root() ;
		assertEquals("Loaders", m.asString("menu.loaders")) ;
		
	}
	
	public void testJsonPrimitive() throws Exception {
		JsonObject json = new JsonObject();
		json.add("text", new JsonPrimitive("allowed pattern : ^[a-z][a-z0-9_]*$"));
		
		Debug.line(json, json.asString("text"));
	}
	
	public void testEscape() throws Exception {
		String str = "allowed pattern : ^[a-z][a-z0-9_]*$" ;
		assertEquals("allowed pattern : ^[a-z][a-z0-9_]*$", String.format(str)) ;

		XMLReader xreader = XMLReaderFactory.createXMLReader();
		String xmlString = IOUtil.toStringWithClose(getClass().getResourceAsStream("messages.test.xml")) ;
		InputSource input = new InputSource(new StringInputStream(xmlString));

		ToJsonHandler handler = new ToJsonHandler();
		xreader.setContentHandler(handler);
		xreader.parse(input);

		assertEquals("allowed pattern : ^[a-z][a-z0-9_]*$", handler.root("unknown").asString("validation.idpattern")) ;
	}
	
}

