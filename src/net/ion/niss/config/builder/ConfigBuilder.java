package net.ion.niss.config.builder;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.ion.niss.config.NSConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ConfigBuilder {

	
	private ServerConfigBuilder sbuilder;
	private RepositoryConfigBuilder rbuilder;
	private LogConfigBuilder lbuilder;
	private SiteSearchConfigBuilder ssbuilder ;

	public ConfigBuilder(){
		this.sbuilder = new ServerConfigBuilder(this) ;
		this.rbuilder = new RepositoryConfigBuilder(this) ;
		this.lbuilder = new LogConfigBuilder(this) ;
		this.ssbuilder = new SiteSearchConfigBuilder(this) ;
	}
	
	
	public NSConfig build(){
		return new NSConfig(sbuilder.build(), rbuilder.build(), lbuilder.build(), ssbuilder.build()) ;
	}
	
	
	public final static ConfigBuilder createDefault(int port) {
		return new ConfigBuilder().sbuilder.port(port).parent();
	}

	public final static ConfigBuilder create(String path) throws IOException {
		try {
			File file = new File(path);
			if ( file.exists() && file.isFile() ){
				;
			} else {
				file = new File("./resource/config/niss-config.xml");
			}
			
			
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);

			XPath xpath = XPathFactory.newInstance().newXPath();

			Node sconfig = (Node) xpath.evaluate("//server-config", document, XPathConstants.NODE);
			Node logconfig = (Node) xpath.evaluate("//log-config-file", document, XPathConstants.NODE);
			Node rconfig = (Node) xpath.evaluate("//repository-config", document, XPathConstants.NODE);
			
			Node ssconfig = (Node) xpath.evaluate("//sitesearch-config", document, XPathConstants.NODE);
			
			return new ConfigBuilder().sbuilder.node(sconfig).parent().lbuilder.node(logconfig).parent().rbuilder.node(rconfig).parent().ssbuilder.node(ssconfig).parent() ;
			
		} catch (SAXException ex) {
			throw new IOException(ex);
		} catch (XPathExpressionException ex) {
			throw new IOException(ex);
		} catch (ParserConfigurationException ex) {
			throw new IOException(ex);
		}
	}

}
