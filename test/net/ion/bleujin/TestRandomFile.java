package net.ion.bleujin;

import java.io.File;
import java.io.RandomAccessFile;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

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
