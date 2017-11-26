package net.ion.niss.config;

import org.w3c.dom.Node;

public class RepositoryConfig {

	private String crakenConfig = "./resource/config/craken-local-config.xml" ;
	private String adminHomeDir ;
	private String indexHomeDir ;
	private String wsName ;
	private String store;
	private Node jdbcNode;
	
	public RepositoryConfig(String crakenConfig, String adminHomeDir, String indexHomeDir, String wsName, String store, Node jdbcNode) { 
		this.crakenConfig = crakenConfig ;
		this.adminHomeDir = adminHomeDir ;
		this.indexHomeDir = indexHomeDir ;
		this.wsName = wsName ;
		this.store = store ;
		this.jdbcNode = jdbcNode ;
	}

	
	public String adminHomeDir(){
		return adminHomeDir ;
	}
	
	public String indexHomeDir(){
		return indexHomeDir ;
	}
	
	public String crakenConfig(){
		return crakenConfig ;
	}
	
	public String wsName(){
		return wsName ;
	}


	public String store() {
		return store;
	}

	public String jdbcUrl() {
		return jdbcNode.getTextContent() ;
	}
	
	public String jdbcId() {
		return jdbcNode.getAttributes().getNamedItem("id").getTextContent() ;
	}
	
	public String jdbcPwd() {
		return jdbcNode.getAttributes().getNamedItem("pwd").getTextContent() ;
	}
	
}
