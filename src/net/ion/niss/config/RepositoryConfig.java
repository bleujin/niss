package net.ion.niss.config;


public class RepositoryConfig {

	private String crakenConfig = "./resource/config/craken-local-config.xml" ;
	private String adminHomeDir ;
	private String indexHomeDir ;
	private String wsName ;
	
	public RepositoryConfig(String crakenConfig, String adminHomeDir, String indexHomeDir, String wsName) { 
		this.crakenConfig = crakenConfig ;
		this.adminHomeDir = adminHomeDir ;
		this.indexHomeDir = indexHomeDir ;
		this.wsName = wsName ;
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

}
