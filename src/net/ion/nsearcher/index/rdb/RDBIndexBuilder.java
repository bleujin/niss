package net.ion.nsearcher.index.rdb;

import java.util.concurrent.ExecutorService;

import net.ion.framework.util.WithinThreadExecutor;
import net.ion.niss.webapp.loaders.RDB;
import net.ion.nsearcher.config.Central;

public class RDBIndexBuilder {

	private Central central;
	private ExecutorService es = new WithinThreadExecutor() ;
	private RDB rdb;
	
	public RDBIndexBuilder(Central central) {
		this.central = central ;
	}

	public static RDBIndexBuilder create(Central cen) {
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
