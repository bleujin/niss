package net.ion.niss.config;

import java.io.File;

import net.ion.framework.db.DBController;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.manager.PostSqlDataSource;

public class SiteSearchConfig {

	private final String jdbcUrl ;
	private final String jdbcId ;
	private final String jdbcPwd ;
	
	private final String screenHomeDir ;
	private final String driverName ;
	private final String driver ;
	
	public SiteSearchConfig(String jdbcUrl, String jdbcId, String jdbcPwd, String screenHomeDir, String driverName, String driver) {
		this.jdbcUrl = jdbcUrl ;
		this.jdbcId = jdbcId ;
		this.jdbcPwd = jdbcPwd ;
		this.screenHomeDir = screenHomeDir ;
		this.driverName = driverName ;
		this.driver = driver ;
		
		System.setProperty(driverName, driver);
		System.setProperty("niss.site.screenHome", screenHomeDir);
	}

	
	public IDBController createDC(){
		PostSqlDataSource dbm = new PostSqlDataSource(jdbcUrl, jdbcId, jdbcPwd) ;
		DBController dc = new DBController(dbm) ;
		return dc ;
	}
	
	public File screenHomeDir(){
		return new File(screenHomeDir) ;
	}

	
	
	
}
