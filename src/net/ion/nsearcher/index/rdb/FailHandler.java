package net.ion.nsearcher.index.rdb;

import java.io.File;
import java.io.IOException;

public interface FailHandler<T> {

	public T onFail(File file, Exception ex)  ;

}
