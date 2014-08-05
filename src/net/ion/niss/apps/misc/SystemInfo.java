package net.ion.niss.apps.misc;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.IOUtil;
import net.ion.nsearcher.config.Central;

import org.apache.log4j.Logger;
import org.apache.lucene.LucenePackage;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;

public class SystemInfo {

	private static Logger log = Logger.getLogger(SystemInfo.class);

	// on some platforms, resolving canonical hostname can cause the thread to block for several seconds if nameservices aren't available
	// so resolve this once per handler instance (ie: not static, so core reload will refresh)
	private String hostname = null;

	public SystemInfo() {
		super();
		try {
			InetAddress addr = InetAddress.getLocalHost();
			hostname = addr.getCanonicalHostName();
		} catch (UnknownHostException e) {
		}
	}

	public JsonObject list() throws Exception {
		JsonObject result = new JsonObject() ;
//		result.add("core", getCoreInfo(req.getCore()));
		result.add("lucene", getLuceneInfo());
		result.add("jvm", getJvmInfo());
		result.add("system", getSystemInfo());
		return result ;
	}

	public static JsonObject getSystemInfo() {
		JsonObject info = new JsonObject();

		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		info.put("name", os.getName());
		info.put("version", os.getVersion());
		info.put("arch", os.getArch());
		info.put("systemLoadAverage", os.getSystemLoadAverage());

		// com.sun.management.OperatingSystemMXBean
		addGetterIfAvaliable(os, "committedVirtualMemorySize", info);
		addGetterIfAvaliable(os, "freePhysicalMemorySize", info);
		addGetterIfAvaliable(os, "freeSwapSpaceSize", info);
		addGetterIfAvaliable(os, "processCpuTime", info);
		addGetterIfAvaliable(os, "totalPhysicalMemorySize", info);
		addGetterIfAvaliable(os, "totalSwapSpaceSize", info);

		// com.sun.management.UnixOperatingSystemMXBean
		addGetterIfAvaliable(os, "openFileDescriptorCount", info);
		addGetterIfAvaliable(os, "maxFileDescriptorCount", info);

		try {
			if (!os.getName().toLowerCase(Locale.ROOT).startsWith("windows")) {
				// Try some command line things
				info.put("uname", execute("uname -a"));
				info.put("uptime", execute("uptime"));
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
		return info;
	}

	static void addGetterIfAvaliable(Object obj, String getter, JsonObject info) {
		// This is a 1.6 function, so lets do a little magic to *try* to make it work
		try {
			String n = Character.toUpperCase(getter.charAt(0)) + getter.substring(1);
			Method m = obj.getClass().getMethod("get" + n);
			m.setAccessible(true);
			Object v = m.invoke(obj, (Object[]) null);
			if (v != null) {
				info.put(getter, v);
			}
		} catch (Exception ex) {
		} // don't worry, this only works for 1.6
	}

	private static String execute(String cmd) {
		DataInputStream in = null;
		Process process = null;

		try {
			process = Runtime.getRuntime().exec(cmd);
			in = new DataInputStream(process.getInputStream());
			// use default charset from locale here, because the command invoked also uses the default locale:
			return IOUtil.toString(new InputStreamReader(in, Charset.defaultCharset()));
		} catch (Exception ex) {
			return "(error executing: " + cmd + ")"; // ignore - log.warn("Error executing command", ex);
		} finally {
			if (process != null) {
				IOUtil.closeQuietly(process.getOutputStream());
				IOUtil.closeQuietly(process.getInputStream());
				IOUtil.closeQuietly(process.getErrorStream());
			}
		}
	}

	/**
	 * Get JVM Info - including memory info
	 */
	public static JsonObject getJvmInfo() {
		JsonObject jvm = new JsonObject();
		jvm.put("version", System.getProperty("java.vm.version"));
		jvm.put("name", System.getProperty("java.vm.name"));

		Runtime runtime = Runtime.getRuntime();
		jvm.put("processors", runtime.availableProcessors());

		// not thread safe, but could be thread local
		DecimalFormat df = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ROOT));

		JsonObject mem = new JsonObject();
		JsonObject raw = new JsonObject();
		long free = runtime.freeMemory();
		long max = runtime.maxMemory();
		long total = runtime.totalMemory();
		long used = total - free;
		double percentUsed = ((double) (used) / (double) max) * 100;
		raw.put("free", free);
		mem.put("free", humanReadableUnits(free, df));
		raw.put("total", total);
		mem.put("total", humanReadableUnits(total, df));
		raw.put("max", max);
		mem.put("max", humanReadableUnits(max, df));
		raw.put("used", used);
		mem.put("used", humanReadableUnits(used, df) + " (%" + df.format(percentUsed) + ")");
		raw.put("used%", percentUsed);

		mem.add("raw", raw);
		jvm.add("memory", mem);

		// JMX properties -- probably should be moved to a different handler
		JsonObject jmx = new JsonObject();
		try {
			RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
			jmx.put("bootclasspath", mx.getBootClassPath());
			jmx.put("classpath", mx.getClassPath());

			// the input arguments passed to the Java virtual machine which does not include the arguments to the main method.
			jmx.put("commandLineArgs", mx.getInputArguments());

			jmx.put("startTime", new Date(mx.getStartTime()));
			jmx.put("upTimeMS", mx.getUptime());

		} catch (Exception e) {
			log.warn("Error getting JMX properties", e);
		}
		jvm.add("jmx", jmx);
		return jvm;
	}

	private static JsonObject getLuceneInfo() {
		JsonObject info = new JsonObject();

		Package searcherPackage = Central.class.getPackage();
		info.put("isearcher-spec-version", searcherPackage.getSpecificationVersion());
		info.put("isearcher-impl-version", searcherPackage.getImplementationVersion());

		Package lucenePackage = LucenePackage.class.getPackage();
		info.put("lucene-spec-version", lucenePackage.getSpecificationVersion());
		info.put("lucene-impl-version", lucenePackage.getImplementationVersion());

		return info;
	}
	
	private static final long ONE_KB = 1024;
	private static final long ONE_MB = ONE_KB * ONE_KB;
	private static final long ONE_GB = ONE_KB * ONE_MB;

	private static String humanReadableUnits(long bytes, DecimalFormat df) {
		String newSizeAndUnits;

		if (bytes / ONE_GB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float) bytes / ONE_GB)) + " GB";
		} else if (bytes / ONE_MB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float) bytes / ONE_MB)) + " MB";
		} else if (bytes / ONE_KB > 0) {
			newSizeAndUnits = String.valueOf(df.format((float) bytes / ONE_KB)) + " KB";
		} else {
			newSizeAndUnits = String.valueOf(bytes) + " bytes";
		}

		return newSizeAndUnits;
	}

}
