package net.ion.nsearcher.index.file;

import java.io.File;
import java.io.IOException;

import net.bleujin.searcher.index.IndexSession;

public interface FileIndexHandler<T> {

	public T onSuccess(IndexSession isession, FileEntry fentry) throws IOException ;
	
	public T onFail(IndexSession isession, File file, Exception ex) ;
}
