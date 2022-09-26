package net.ion.nsearcher.index.rdb;

import java.util.concurrent.ExecutorService;

import net.bleujin.searcher.SearchController;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.niss.webapp.loaders.RDB;

public class RDBIndexBuilder {

	private SearchController central;
	private ExecutorService es = new WithinThreadExecutor() ;
	private RDB rdb;
	
	public RDBIndexBuilder(SearchController central) {
		this.central = central ;
	}

	public static RDBIndexBuilder create(SearchController cen) {
		return new RDBIndexBuilder(cen);
	}
	
	public RDBIndexBuilder executors(ExecutorService es){
		this.es = es ;
		return this ;
	}
	
	public RDBIndexBuilder rdb(RDB rdb){
		this.rdb = rdb ;
		return this ;
	}
	
	
	public RDBIndexer build(){
		return new RDBIndexer(this.central, this.rdb, this.es) ;
	}

}
