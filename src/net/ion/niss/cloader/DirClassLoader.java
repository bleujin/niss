package net.ion.niss.cloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * This utility class loader allows to load and execute classes from the directory. During initialisation it will load all jar files in the directory and than it will be able to load these classes on demand
 */
public class DirClassLoader extends ClassLoader {
	// All details about resource
	private class ResourceDescriptor {
		public String dirPath; // Full path to the base directory which has this resource
		public String jarPath; // Full path to the jar which has this resource
		public String contentPath; // Path to the resource inside jar or just a path to it (if it was in the directory and not in the jar)
		public URL resourceURL; // The URL we use to uniquely identify the resource
		public byte[] binaryContent;
	}

	private Map<String, List> resourceNamesMap = new HashMap<String, List>(); // Contains information about all resources. Key - resource name, value Set with Resource Descriptors
	private Map<String, ResourceDescriptor> classesMap = new HashMap<String, ResourceDescriptor>(); // Contains information about resources which represent classes. Key - class name, ResourceDescriptor
	private Map<String, Class> alreadyLoadedClasses = new HashMap<String, Class>(); // Key - class name. Contents class

	public DirClassLoader(String homeDirectory) throws IOException {
		this(new File(homeDirectory));
	}

	public DirClassLoader(File homeDirectory) throws IOException {
		super(DirClassLoader.class.getClassLoader());
		readDirectory(homeDirectory.getAbsoluteFile());
	}

	
	public DirClassLoader(String homeDirectory, ClassLoader parentClassLoader) throws IOException {
		super(parentClassLoader);
		readDirectory(new File(homeDirectory).getAbsoluteFile());
	}

	public void addDirectory(String additionalDirectory) throws IOException {
		readDirectory(new File(additionalDirectory).getAbsoluteFile());
	}

	/** Iterates through the directory and reads all jars */
	private void readDirectory(File fromDirectory) throws IOException {
		if (!fromDirectory.exists())
			throw new IllegalArgumentException("Directory " + fromDirectory.getAbsolutePath() + " does not exists");
		if (!fromDirectory.isDirectory())
			throw new IllegalArgumentException(fromDirectory.getAbsolutePath() + " is not a directory");
		File[] listFiles = fromDirectory.listFiles(new FileFilter() {
			public boolean accept(File pPathname) {
				return pPathname.isFile();
			}
		});
		// Load all files one by one
		for (int i = 0; i < listFiles.length; i++) {
			File f = listFiles[i];
			if (f.getName().toLowerCase().endsWith(".jar")){
				addJar(f);
			} else {
				addResource(f, fromDirectory.getAbsolutePath());
			}
		}
	}

	/** This method will take passed in-memory jar file and add it to its library */
	private void addResource(File resource, String rootDirectoryPath) throws java.io.IOException {
		if (!resource.getAbsolutePath().startsWith(rootDirectoryPath))
			throw new java.io.IOException("File " + resource.getAbsolutePath() + " is not in the " + rootDirectoryPath + " directory.");
		// Chop off the root directory path and force file separators
		String resourceName = StringUtils.replace(resource.getAbsolutePath().substring(rootDirectoryPath.length() + 1), "\\", "/");

		List descriptorList = (List) resourceNamesMap.get(resourceName);
		if (descriptorList == null)
			resourceNamesMap.put(resourceName, descriptorList = new ArrayList());
		ResourceDescriptor descriptor = new ResourceDescriptor();
		descriptor.dirPath = StringUtils.replace(rootDirectoryPath, "\\", "/");
		descriptor.contentPath = resourceName;
		descriptor.resourceURL = resource.toURL();
		descriptor.binaryContent = IOUtil.toByteArray(new FileInputStream(resource));
		descriptorList.add(descriptor);
		// See if we need to store the resource in the java class maps
		if (resourceName.toLowerCase().endsWith(".class")) {
			String className = resourceName.substring(0, resourceName.length() - 6); // Prepare java style class name - strip .class from the end
			className = StringUtils.replace(className, "/", "."); // Ensure that all slashes (back and forward) are converted to dots
			classesMap.put(className, descriptor);
		}
	}

	/** This method will take passed in-memory jar file and add it to its library */
	private void addJar(File jarFile) throws java.io.IOException {
		JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
		JarEntry entry = null;
		String jarPath = jarFile.getAbsolutePath();
		URL jarURL = jarFile.toURL();
		while ((entry = jarInputStream.getNextJarEntry()) != null) {
			byte[] binaryContent = IOUtil.toByteArray(jarInputStream);
			if (binaryContent != null && binaryContent.length > 0) {
				String resourceName = StringUtils.replace(entry.getName(), "\\", "/"); // Ensure that all back slashes are converted to forward slashes (just for standartisation)
				List resourceDescriptorList = (List) resourceNamesMap.get(resourceName);
				if (resourceDescriptorList == null)
					resourceNamesMap.put(resourceName, resourceDescriptorList = new ArrayList());
				ResourceDescriptor resourceDescriptor = new ResourceDescriptor();
				resourceDescriptor.jarPath = jarPath;
				resourceDescriptor.contentPath = resourceName;
				try {
					resourceDescriptor.resourceURL = new URL(jarURL.toString() + "#JarEntry=" + IOUtil.toString(entry.getName().getBytes()));
				} catch (MalformedURLException ignore) {
					// Ignore as we are trying to fake it
				}
				resourceDescriptor.binaryContent = binaryContent;
				resourceDescriptorList.add(resourceDescriptor);
				// See if we need to store the resource in the java class maps
				if (resourceName.toLowerCase().endsWith(".class")) {
					// Prepare java style class name - strip .class from the end
					String className = resourceName.substring(0, resourceName.length() - 6);
					// Ensure that all slashes (back and forward) are converted to dots
					className = StringUtils.replace(className, "/", ".");
					classesMap.put(className, resourceDescriptor);
				}
			}
		}
	}

	/** Returns true if this classloader has the specified class */
	public boolean hasClass(String className) {
		return classesMap.containsKey(className);
	}

	/**
	 * This method is a part of ClassLoader mechanism. Overridden here to load classes from stored ones
	 */
	public Class findClass(String className) throws ClassNotFoundException {
		ResourceDescriptor descriptor = (ResourceDescriptor) classesMap.get(className); // construct the name of the class file to look for
		if (descriptor != null)
			return defineClass(className, descriptor.binaryContent, 0, descriptor.binaryContent.length);
		throw new ClassNotFoundException(className);
	}

	protected URL findResource(String resourceName) {
		List resourceDescriptorList = (List) resourceNamesMap.get(resourceName); // Supply first resource
		if (resourceDescriptorList != null && resourceDescriptorList.size() > 0) {
			ResourceDescriptor resourceDescriptor = (ResourceDescriptor) resourceDescriptorList.get(0);
			return resourceDescriptor.resourceURL;
		}
		return null;
	}

	protected Enumeration findResources(String resourceName) throws IOException {
		List urlList = new ArrayList(); // Supply all resources
		List resourceDescriptorList = (List) resourceNamesMap.get(resourceName);
		if (resourceDescriptorList != null && resourceDescriptorList.size() > 0) {
			Iterator descriptorsIter = resourceDescriptorList.iterator();
			while (descriptorsIter.hasNext()) {
				ResourceDescriptor resourceDescriptor = (ResourceDescriptor) descriptorsIter.next();
				urlList.add(resourceDescriptor.resourceURL);
			}
		}
		Enumeration otherResourcePaths = super.findResources(resourceName);
		while (otherResourcePaths.hasMoreElements())
			urlList.add(otherResourcePaths.nextElement());
		return Collections.enumeration(urlList);
	}

	public InputStream getResourceAsStream(String resourceName) {
		List resourceDescriptorList = (List) resourceNamesMap.get(resourceName);
		if (resourceDescriptorList != null && resourceDescriptorList.size() > 0) {
			ResourceDescriptor resourceDescriptor = (ResourceDescriptor) resourceDescriptorList.get(0);
			return new ByteArrayInputStream(resourceDescriptor.binaryContent);
		}
		ClassLoader parentLoader = getParent();
		if (parentLoader == null)
			parentLoader = getSystemClassLoader();
		return parentLoader.getResourceAsStream(resourceName);
	}

	private static class StringUtils {

		public static String replace(String text, String searchString, String replacement) {
			return replace(text, searchString, replacement, -1);
		}

		public static String replace(String text, String searchString, String replacement, int max) {
			if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0)
				return text;
			int start = 0;
			int end = text.indexOf(searchString, start);
			if (end == -1)
				return text;
			int replLength = searchString.length();
			int increase = replacement.length() - replLength;
			increase = increase >= 0 ? increase : 0;
			increase *= max >= 0 ? max <= 64 ? max : 64 : 16;
			StringBuffer buf = new StringBuffer(text.length() + increase);
			do {
				if (end == -1)
					break;
				buf.append(text.substring(start, end)).append(replacement);
				start = end + replLength;
				if (--max == 0)
					break;
				end = text.indexOf(searchString, start);
			} while (true);
			buf.append(text.substring(start));
			return buf.toString();
		}

		public static boolean isEmpty(String str) {
			return str == null || str.length() == 0;
		}
	}

	static class IOUtil {

		public static byte[] toByteArray(InputStream input) throws IOException {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			copy(input, output);
			return output.toByteArray();
		}

		public static String toString(byte[] bytes) {
			return new String(bytes);
		}

		public static int copy(InputStream input, OutputStream output) throws IOException {
			long count = copyLarge(input, output);
			if (count > 2147483647L)
				return -1;
			else
				return (int) count;
		}

		public static long copyLarge(InputStream input, OutputStream output) throws IOException {
			byte buffer[] = new byte[4096];
			long count = 0L;
			for (int n = 0; -1 != (n = input.read(buffer));) {
				output.write(buffer, 0, n);
				count += n;
			}

			return count;
		}

	}
}
