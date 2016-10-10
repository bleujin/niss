package net.ion.niss.webapp.sites;

import java.util.concurrent.atomic.AtomicInteger;

import net.ion.framework.util.Debug;
import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Task;
import net.ion.icrawler.pipeline.Pipeline;

public class OutPipeline implements Pipeline {
	private AtomicInteger count = new AtomicInteger();

	public void process(ResultItems ritems, Task task) {
		Debug.line(count.incrementAndGet(), ritems.getRequest().getUrl(), ritems.asString("title"));
	}
}