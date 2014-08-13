package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.manager.DefaultCacheManager;

import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;

public class REntry implements Closeable{

	public final static String EntryName = "rentry" ;
	
	private RepositoryImpl r;
	private String wsName;

	public REntry(RepositoryImpl r, String wsName) {
		this.r = r ;
		this.wsName = wsName ;
	}

	public final static REntry test() throws CorruptIndexException, IOException{
//		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
//		r.defineWorkspaceForTest(wsname, ISearcherWorkspaceConfig.create().location(""));
//		r.start();
		
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;
		return new REntry(r, "test") ;
	}
	
	public ReadSession login() throws IOException{
		return r.login(wsName) ;
	}
	
	public RepositoryImpl repository(){
		return r ;
	}

	@Override
	public void close() throws IOException {
		r.shutdown() ;
	}
	
}
