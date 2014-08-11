package net.ion.niss.apps;

import java.io.IOException;

import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.crud.RepositoryImpl;

import org.infinispan.manager.DefaultCacheManager;

public class Store {

	private final static String wsname = "admin" ;
	private RepositoryImpl r;
	private Store(RepositoryImpl r) {
		this.r = r ;
	}

	public final static Store test() throws IOException{
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest(wsname, ISearcherWorkspaceConfig.create().location(""));
		r.start();
		return new Store(r) ;
	}

	public ReadSession login() throws IOException {
		return r.login("admin");
	}
}
