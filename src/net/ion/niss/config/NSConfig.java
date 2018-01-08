package net.ion.niss.config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.ion.niss.webapp.REntry;


public class NSConfig {

	private ServerConfig serverConfig;
	private LogConfig logConfig;
	private RepositoryConfig repoConfig ;
	private SiteSearchConfig siteConfig;
	
	public NSConfig(ServerConfig serverConfig, RepositoryConfig repoConfig, LogConfig logConfig, SiteSearchConfig siteConfig) {
		this.serverConfig = serverConfig ;
		this.repoConfig = repoConfig ;
		this.logConfig = logConfig;
		this.siteConfig = siteConfig ;
	}

	
	public ServerConfig serverConfig(){
		return serverConfig ;
	}

	public LogConfig logConfig(){
		return logConfig ;
	}


	public RepositoryConfig repoConfig() {
		return repoConfig ;
	}

	public SiteSearchConfig siteConfig(){
		return siteConfig ;
	}

	
	public REntry createREntry() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Craken craken = repoConfig.crakenConfig().build().start() ;

		return new REntry(craken, repoConfig.wsName(), this);
	}


	public REntry testREntry() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Craken r = CrakenConfig.mapMemory().build() ;
		r.start();

		return new REntry(r, "test", this);
	}


}
