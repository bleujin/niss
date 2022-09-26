package net.ion.nsearcher.index.rdb;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bleujin.searcher.index.IndexSession;
import net.ion.niss.webapp.loaders.RDB;

public interface RDBIndexHandler<T> {

	public T onSuccess(IndexSession isession, RDB rdb, ResultSet rs) throws IOException, SQLException ;
	
	public T onFail(IndexSession isession, RDB rdb, Exception ex) ;
}
