package net.ion.framework.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class FileUtil extends FileUtils {
	private final static class configFilter implements FileFilter {
		public boolean accept(File file) {
			return file.getName().startsWith("plugin") && file.getName().endsWith(".xml");
		}
	}

	private final static class DirFilter implements FileFilter {
		public boolean accept(File file) {
			return file.isDirectory();
		}
	}

	public static FileFilter DIR_FILTER = new DirFilter();
	public static FileFilter PLUGIN_CONFIG_FILE_FILTER = new configFilter();

	public static File findFile(File parentDir, FileFilter filter, boolean recursive) {
		if (filter.accept(parentDir)) {
			return parentDir;
		}

		if (parentDir.isDirectory() && recursive) {
			for (File child : parentDir.listFiles()) {
				File found = findFile(child, filter, recursive);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	public static Iterable<String> readLineIterable(Reader reader) throws IOException {
		final BufferedReader br = new BufferedReader(reader);

		return new Iterable<String>() {
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					public boolean hasNext() {
						return line != null;
					}

					public String next() {
						String retval = line;
						line = getLine();
						return retval;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}

					String getLine() {
						String line = null;
						try {
							line = br.readLine();
						} catch (IOException ioEx) {
							line = null;
						}
						return line;
					}

					String line = getLine();
				};
			}
		};
	}

	public static File[] findFiles(File parentDir, FileFilter filter, boolean recursive) {
		List<File> result = ListUtil.newList();
		if (filter.accept(parentDir)) {
			result.add(parentDir);
		}

		if (parentDir.isDirectory() && recursive) {
			for (File child : parentDir.listFiles()) {
				for (File find : findFiles(child, filter, recursive)) {
					result.add(find);
				}
				;
			}
		}
		return result.toArray(new File[0]);
	}

	public static boolean forceMkdirQuietly(File directory) {
		if (!directory.exists()) {
			synchronized (Shell.class) {
				if (!directory.exists()) {
					return directory.mkdirs();
				}
			}
		} else {
			if(directory.isFile()){
				return false;
			}
		}
		return true;
	}
	
	public static boolean forceWriteUTF8(File file, String content) throws IOException{
		boolean created = true ;
		if (!file.getParentFile().exists()){
			created = file.getParentFile().mkdirs() ;
		}
		if (created){
			FileUtils.writeStringToFile(file, content, "UTF-8");
		}
		return created ;
	}
	
	public interface FileClosure<V> {
		public V walk(File file) ;
	}

}
