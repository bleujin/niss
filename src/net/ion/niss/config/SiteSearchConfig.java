package net.ion.niss.config;

import java.io.File;

public class SiteSearchConfig {

	private final String screenHomeDir ;
	private final String driverName ;
	private final String driver ;
	
	public SiteSearchConfig(String screenHomeDir, String driverName, String driver) {
		this.screenHomeDir = screenHomeDir ;
		this.driverName = driverName ;
		this.driver = driver ;
		
		System.setProperty(driverName, driver);
		System.setProperty("niss.site.screenHome", screenHomeDir);
	}

	
	public File screenHomeDir(){
		return new File(screenHomeDir) ;
	}

	
	
	
}
