package net.ion.bleujin;

import java.io.File;
import java.io.StringReader;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;

public class TestXPath extends TestCase {

	
	public void testConfigFile() throws Exception {
		NSConfig nsconfig = ConfigBuilder.createDefault(9000).build() ;
		
		assertEquals(9000, nsconfig.serverConfig().port());
		assertEquals("niss", nsconfig.serverConfig().id());
		assertEquals("./resource/log4j.properties", nsconfig.logConfig().fileLoc());
	}
	
	public void testXPath() throws Exception {
		String xml = "<root><row>"
				+ "	<col1 id='c1'>값1</col1>"
				+ "	<col2 id='c2' val='val2'>값2</col2>"
				+ "</row>"
				+ "<row>"
				+ "	<col1 id='c3'>값3</col1>"
				+ "	<col2 id='c4'>값4</col2>"
				+ "</row></root>";

		// XML Document 객체 생성
		InputSource is = new InputSource(new StringReader(xml));
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

		// 인터넷 상의 XML 문서는 요렇게 생성하면 편리함.
		// Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("http://www.example.com/test.xml");
		// xpath 생성
		XPath xpath = XPathFactory.newInstance().newXPath();

		// NodeList 가져오기 : row 아래에 있는 모든 col1 을 선택
		// 값1 값3 이 출력됨
		NodeList cols = (NodeList) xpath.evaluate("//row/col1", document, XPathConstants.NODESET);
		for (int idx = 0; idx < cols.getLength(); idx++) {
			System.out.println(cols.item(idx).getTextContent());
		}

		// id 가 c2 인 Node의 val attribute 값 가져오기
		Node col2 = (Node) xpath.evaluate("//*[@id='c2']", document, XPathConstants.NODE);
		assertEquals("val2", col2.getAttributes().getNamedItem("val").getTextContent());

		// id 가 c3 인 Node 의 value 값 가져오기
		assertEquals("값3", xpath.evaluate("//*[@id='c3']", document, XPathConstants.STRING));
	}
	
	public void testJarURL() throws Exception {
		Collection<File> jarfiles = FileUtil.listFiles(new File("./lib"),  new String[]{"jar"}, true) ;
		for (File f : jarfiles) {
			Debug.line(f);
		}
	}
	
	

}
