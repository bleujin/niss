package net.ion.niss.config;

import java.io.File;

import org.w3c.dom.Node;

import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;

public class RepositoryConfig {

	private final Craken craken ;
	private String adminHomeDir ;
	private String indexHomeDir ;
	private String wsName ;

	public RepositoryConfig(Craken craken, String adminHomeDir, String indexHomeDir, String wsName) { 
		this.craken = craken ;
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
	
	public Craken craken(){
		return craken ;
	}
	
	public String wsName(){
		return wsName ;
	}


}
