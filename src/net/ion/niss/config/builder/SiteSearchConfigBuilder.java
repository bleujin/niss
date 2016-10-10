package net.ion.niss.config.builder;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.ion.niss.config.SiteSearchConfig;

import org.apache.ecs.xhtml.del;
import org.w3c.dom.Node;

public class SiteSearchConfigBuilder {

	private ConfigBuilder parent;
	
	private String jdbcUrl ;
	private String jdbcId ;
	private String jdbcPwd ;
	
	private String screenHomeDir ;
	private String driverName ;
	private String driver ;
	
	public SiteSearchConfigBuilder(ConfigBuilder parent){
		this.parent = parent ;
	}
	
	public SiteSearchConfigBuilder node(Node ssconfig) throws XPathExpressionException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		Node jdbcNode 	= (Node) xpath.evaluate("jdbcurl", ssconfig, XPathConstants.NODE);
		Node screenNode = (Node) xpath.evaluate("screen-home", ssconfig, XPathConstants.NODE);
		
		return 	jdbcUrl(jdbcNode.getTextContent(), jdbcNode.getAttributes().getNamedItem("id").getTextContent(), jdbcNode.getAttributes().getNamedItem("pwd").getTextContent())
				.screen(screenNode.getTextContent(), screenNode.getAttributes().getNamedItem("name").getTextContent(), screenNode.getAttributes().getNamedItem("driver").getTextContent()) ;
	}
	
	public SiteSearchConfigBuilder jdbcUrl(String jdbcUrl, String id, String pwd){
		this.jdbcUrl = jdbcUrl ;
		this.jdbcId = id ;
		this.jdbcPwd = pwd ;
		return this ;
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
		return new SiteSearchConfig(jdbcUrl, jdbcId, jdbcPwd, screenHomeDir, driverName, driver);
	}
	
}
