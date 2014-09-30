package net.ion.niss.webapp;

import java.io.File;
import java.io.RandomAccessFile;

import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestRandomFile extends TestCase {

	
	public void testWrite() throws Exception {
		File file = File.createTempFile("ddd", "test") ;
		RandomAccessFile raf = new RandomAccessFile(file, "rw") ;
		raf.writeUTF("Hello ");
		raf.writeUTF("World");
		raf.seek(0);
		
	
		Debug.line(raf.readUTF()) ;
		Debug.line(raf.readUTF()) ;
	}
}