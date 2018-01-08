package net.ion.niss.config.builder;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;

import net.ion.niss.config.SiteSearchConfig;

public class SiteSearchConfigBuilder {

	private ConfigBuilder parent;
	
	private String screenHomeDir ;
	private String driverName ;
	private String driver ;
	
	public SiteSearchConfigBuilder(ConfigBuilder parent){
		this.parent = parent ;
	}
	
	public SiteSearchConfigBuilder node(Node ssconfig) throws XPathExpressionException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		Node screenNode = (Node) xpath.evaluate("screen-home", ssconfig, XPathConstants.NODE);
		
		return 	screen(screenNode.getTextContent(), screenNode.getAttributes().getNamedItem("name").getTextContent(), screenNode.getAttributes().getNamedItem("driver").getTextContent()) ;
	}
	
	public SiteSearchConfigBuilder screen(String screenHomeDir, String driverName, String driver){
		this.screenHomeDir = screenHomeDir ;
		this.driverName = driverName ;
		this.driver = driver ;
		return this ;
	}
	
	public ConfigBuilder parent() {
		return parent;
	}

	public SiteSearchConfig build() {
		return new SiteSearchConfig(screenHomeDir, driverName, driver);
	}
	
}
