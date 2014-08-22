package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Locale;

import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.niss.webapp.util.SimpleOrderedMap;

public class ThreadDumpInfo {

	public JsonObject list() throws IOException {
		JsonObject result = new JsonObject();

		ThreadMXBean tmbean = ManagementFactory.getThreadMXBean();

		// Thread Count
		JsonObject nl = new JsonObject().put("current", tmbean.getThreadCount()).put("peak", tmbean.getPeakThreadCount()).put("daemon", tmbean.getDaemonThreadCount());
		result.put("info", nl);

		// Deadlocks
		ThreadInfo[] deadThreadInfo;
		long[] deadThreadIds = tmbean.findMonitorDeadlockedThreads();
		if (deadThreadIds != null) {
			deadThreadInfo = tmbean.getThreadInfo(deadThreadIds, Integer.MAX_VALUE);
			JsonArray threads = new JsonArray();
			for (ThreadInfo tinfo : deadThreadInfo) {
				if (tinfo != null) {
					threads.add(threadInfo(tinfo, tmbean));
				}
			}
			result.put("deadlocks", threads);
		}

		// Now show all the threads....
		long[] allThreadIds = tmbean.getAllThreadIds();
		ThreadInfo[] allThreadInfo = tmbean.getThreadInfo(allThreadIds, Integer.MAX_VALUE);
		JsonArray threads = new JsonArray();
		for (ThreadInfo tinfo : allThreadInfo) {
			if (tinfo != null) {
				threads.add(threadInfo(tinfo, tmbean));
			}
		}
		result.put("threadDump", threads);
		return result;
	}


	private JsonObject threadInfo(ThreadInfo ti, ThreadMXBean tmbean) {
		JsonObject info = new JsonObject();
		long tid = ti.getThreadId();

		info.put("id", tid);
		info.put("name", ti.getThreadName());
		info.put("state", ti.getThreadState().toString());

		if (ti.getLockName() != null) {
			info.put("lock", ti.getLockName());
		}
		if (ti.isSuspended()) {
			info.put("suspended", true);
		}
		if (ti.isInNative()) {
			info.put("native", true);
		}

		if (tmbean.isThreadCpuTimeSupported()) {
			info.put("cpuTime", formatNanos(tmbean.getThreadCpuTime(tid)));
			info.put("userTime", formatNanos(tmbean.getThreadUserTime(tid)));
		}

		if (ti.getLockOwnerName() != null) {
			SimpleOrderedMap<Object> owner = new SimpleOrderedMap<Object>();
			owner.add("name", ti.getLockOwnerName());
			owner.add("id", ti.getLockOwnerId());
		}

		// Add the stack trace
		int i = 0;
		JsonArray trace = new JsonArray();
		for (StackTraceElement ste : ti.getStackTrace()) {
			trace.add(new JsonPrimitive(ste.toString()));
		}
		info.put("stackTrace", trace);
		return info;
	}

	private String formatNanos(long ns) {
		return String.format(Locale.ROOT, "%.4fms", ns / (double) 1000000);
	}
}
