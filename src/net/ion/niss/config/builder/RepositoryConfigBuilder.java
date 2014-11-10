package net.ion.niss.config.builder;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.ion.framework.util.StringUtil;
import net.ion.niss.config.RepositoryConfig;

import org.w3c.dom.Node;

public class RepositoryConfigBuilder {

	private String wsName = "admin" ;
	private String adminHomeDir = "./resource/admin/" ;
	private String indexHomeDir = "./resource/index/" ;
	private ConfigBuilder parent;
	private String crakenConfig = "./resource/config/craken-local-config.xml";

	public RepositoryConfigBuilder(ConfigBuilder parent){
		this.parent = parent ;
	}
	
	public RepositoryConfigBuilder node(Node rconfig) throws XPathExpressionException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		
		Node configNode = (Node) xpath.evaluate("craken-config", rconfig, XPathConstants.NODE);
		Node adminNode = (Node) xpath.evaluate("admin-home", rconfig, XPathConstants.NODE);
		Node indexNode = (Node) xpath.evaluate("index-home", rconfig, XPathConstants.NODE);
		String wname = rconfig.getAttributes().getNamedItem("wsname").getTextContent() ;
		
		return configLoc(configNode.getTextContent()).adminHomeDir(adminNode.getTextContent()).indexHomeDir(indexNode.getTextContent()).wsName(wname);
	}

	public RepositoryConfigBuilder adminHomeDir(String adminHomeDir){
		this.adminHomeDir = StringUtil.defaultIfEmpty(adminHomeDir, "./resource/admin/") ;
		return this ;
	}

	public RepositoryConfigBuilder configLoc(String configLoc){
		this.crakenConfig = StringUtil.defaultIfEmpty(configLoc, "./resource/config/craken-local-config.xml") ;
		return this ;
	}

	
	public RepositoryConfigBuilder indexHomeDir(String indexHomeDir){
		this.indexHomeDir = StringUtil.defaultIfEmpty(indexHomeDir, "./resource/index/") ;
		return this ;
	}

	public RepositoryConfigBuilder wsName(String wsName){
		this.wsName = StringUtil.defaultIfEmpty(wsName, "admin") ;
		return this ;
	}
	
	
	public ConfigBuilder parent(){
		return parent ;
	}


	public RepositoryConfig build() {
		return new RepositoryConfig(crakenConfig, adminHomeDir, indexHomeDir, wsName);
	}
	
	
}
