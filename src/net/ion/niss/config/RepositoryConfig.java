package net.ion.niss.config;

import java.io.File;

import org.w3c.dom.Node;

import net.bleujin.rcraken.CrakenConfig;

public class RepositoryConfig {

	private final CrakenConfig crakenConfig ;
	private String adminHomeDir ;
	private String indexHomeDir ;
	private String wsName ;

	public RepositoryConfig(CrakenConfig crakenConfig, String adminHomeDir, String indexHomeDir, String wsName) { 
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
	
	public CrakenConfig crakenConfig(){
		return crakenConfig ;
	}
	
	public String wsName(){
		return wsName ;
	}


}
