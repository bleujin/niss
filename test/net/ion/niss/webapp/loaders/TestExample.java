package net.ion.niss.webapp.loaders;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.niss.webapp.REntry;
import net.ion.niss.webapp.loaders.example.ExampleDummy;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.client.StubServer;

public class TestExample extends TestCase {

	private StubServer ss;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.ss = StubServer.create(LoaderWeb.class);
		ss.treeContext().putAttribute(REntry.EntryName, REntry.test()) ;
	}
	
	public void testCall() throws Exception {
		StubHttpResponse response = ss.request("/loaders/11/examples").get() ;
		Debug.line(response.contentsString()) ;
	}

	

	public void testList() throws Exception {
		Debug.line( resourceList(ExampleDummy.class, "net/ion/niss/webapp/loaders/example"));
		Debug.line( resourceList(TestCase.class, "junit/framework/"));
	}

	private String[] resourceList(Class clazz, String path) throws URISyntaxException, IOException {
		URL dirURL = clazz.getClassLoader().getResource(path);
		if (dirURL != null && dirURL.getProtocol().equals("file")) {
			/* A file path: easy enough */
			return new File(dirURL.toURI()).list();
		}

		if (dirURL == null) {
			String me = clazz.getName().replace(".", "/") + ".class";
			dirURL = clazz.getClassLoader().getResource(me);
		}

		if (dirURL.getProtocol().equals("jar")) {
			/* A JAR path */
			String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); // strip out only the JAR file
			JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
			Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
			Set<String> result = new HashSet<String>(); // avoid duplicates in case it is a subdirectory
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				if (name.startsWith(path)) { // filter according to the path
					String entry = name.substring(path.length());
					int checkSubdir = entry.indexOf("/");
					if (checkSubdir > 0) {
						// if it is a subdirectory, we just return the directory name
						entry = entry.substring(0, checkSubdir);
					}
					result.add(entry);
				}
			}
			return result.toArray(new String[result.size()]);
		}

		throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
	}
}
