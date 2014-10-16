package net.ion.niss.config;

import java.io.IOException;

import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.niss.webapp.REntry;

import org.infinispan.manager.DefaultCacheManager;


public class NSConfig {

	private ServerConfig serverConfig;
	private LogConfig logConfig;
	private RepositoryConfig repoConfig ;
	
	public NSConfig(ServerConfig serverConfig, RepositoryConfig repoConfig, LogConfig logConfig) {
		this.serverConfig = serverConfig ;
		this.repoConfig = repoConfig ;
		this.logConfig = logConfig;
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

	
	public REntry createREntry() throws IOException{
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), serverConfig().id());
		r.defineWorkspaceForTest(repoConfig.wsName(), ISearcherWorkspaceConfig.create().location(repoConfig.adminHomeDir()));
		r.start();

		return new REntry(r, repoConfig.wsName(), this);
	}


	public REntry testREntry() throws IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), serverConfig.id());
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create().location(""));
		r.start();

		return new REntry(r, "test", this);
	}
	
}
