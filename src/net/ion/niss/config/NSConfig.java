package net.ion.niss.config;

import java.io.IOException;

import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.FileSystemWorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.db.IDBController;
import net.ion.niss.webapp.REntry;

import org.infinispan.manager.DefaultCacheManager;


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
	
	public IDBController createDC(){
		return siteConfig.createDC();
	}
	
	public REntry createREntry() throws IOException{
		Craken r = Craken.create(new DefaultCacheManager(repoConfig.crakenConfig()), serverConfig.id());
		if ("fs".equals(repoConfig.store())){
			r.createWorkspace(repoConfig.wsName(), new FileSystemWorkspaceConfigBuilder(repoConfig.adminHomeDir()));
		} else {
			r.createWorkspace(repoConfig.wsName(), WorkspaceConfigBuilder.gridDir(repoConfig.adminHomeDir()));
		}
		r.start();

		return new REntry(r, repoConfig.wsName(), this);
	}


	public REntry testREntry() throws IOException {
		Craken r = Craken.inmemoryCreateWithTest() ;
		r.start();

		return new REntry(r, "test", this);
	}
	
}
