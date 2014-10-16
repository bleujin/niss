package net.ion.niss.config;


public class RepositoryConfig {

	private String adminHomeDir ;
	private String indexHomeDir ;
	private String wsName ;
	
	public RepositoryConfig(String adminHomeDir, String indexHomeDir, String wsName) {
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
	
	public String wsName(){
		return wsName ;
	}

}
