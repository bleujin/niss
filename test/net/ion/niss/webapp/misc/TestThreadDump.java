package net.ion.niss.webapp.misc;

import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Type;
import java.util.Map.Entry;

import net.ion.framework.parse.gson.Gson;
import net.ion.framework.parse.gson.GsonBuilder;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.misc.ThreadDumpInfo;
import net.ion.niss.webapp.util.SimpleOrderedMap;
import junit.framework.TestCase;

public class TestThreadDump extends TestCase {

	public void testViewDump() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		bean.setThreadCpuTimeEnabled(true);

		long[] threadIds = bean.getAllThreadIds();
		ThreadInfo[] allInfos = bean.getThreadInfo(threadIds, Integer.MAX_VALUE);
		for (ThreadInfo ti : allInfos) {
			if (ti == null) {
				continue;
			}

			if (ti.getThreadName().equals(Thread.currentThread().getName())) {
				continue;
			}
			System.out.println("Thread " + ti.getThreadId() + " " + ti.getThreadName() + " " + ti.getThreadState());
			System.out.println("Stacktrace:");
			StackTraceElement[] st = ti.getStackTrace();
			for (StackTraceElement ste : st) {
				System.out.println(ste.getClassName() + "." + ste.getMethodName() + " line: " + ste.getLineNumber());
			}
			Debug.line(); 
		}
	}
	
	public void testDump() throws Exception {
		ThreadDumpInfo info = new ThreadDumpInfo() ;
		JsonObject json = info.list() ;
		
		StringWriter writer = new StringWriter();
		new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
		
		Debug.line(writer);
	}
	
	public void testPackageInfo() throws Exception {
		Debug.line(TestThreadDump.class.getPackage().getSpecificationVersion()) ;
	}

}
