package net.ion.niss.webapp.misc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.infinispan.commons.util.ReflectionUtil;

import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.icrawler.utils.ClassUtils;
import net.ion.niss.webapp.Webapp;
import junit.framework.TestCase;

public class TestAnalysisWeb extends TestCase {
	

	public void testLoad() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Webapp.ANALYSIS_FILE), "UTF-8")) ;
		String line = null ;
		while((line = reader.readLine()) != null) {
			if (line.startsWith("//")) continue ;
			Debug.line(line);
		}
		
		
	}
}
