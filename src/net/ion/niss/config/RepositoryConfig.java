package net.ion.niss.config;


public class RepositoryConfig {

	private String crakenConfig = "./resource/config/craken-local-config.xml" ;
	private String adminHomeDir ;
	private String indexHomeDir ;
	private String wsName ;
	private String store;
	
	public RepositoryConfig(String crakenConfig, String adminHomeDir, String indexHomeDir, String wsName, String store) { 
		this.crakenConfig = crakenConfig ;
		this.adminHomeDir = adminHomeDir ;
		this.indexHomeDir = indexHomeDir ;
		this.wsName = wsName ;
		this.store = store ;
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

}
