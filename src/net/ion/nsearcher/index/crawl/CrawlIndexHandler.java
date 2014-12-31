package net.ion.nsearcher.index.crawl;

import java.io.IOException;

import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Task;
import net.ion.nsearcher.index.IndexSession;

public interface CrawlIndexHandler<T> {
	
	public T onSuccess(IndexSession isession, ResultItems ritems, Task task) throws IOException;

	public T onFail(IndexSession isession, ResultItems ritems, Task task, IOException e);
}
