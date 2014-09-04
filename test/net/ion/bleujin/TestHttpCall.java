package net.ion.bleujin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import junit.framework.TestCase;

public class TestHttpCall extends TestCase {

	
	public void testCall() throws Exception {
		Socket socket = new Socket(InetAddress.getByName("61.250.201.157"), 9000) ;
		OutputStream output = socket.getOutputStream() ;
		OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8") ;
		writer.write("GET /img/avatars/sunny.png HTTP/1.0\n");
		writer.write("Host: www.daum.net\n\n");
		writer.flush(); 
		 
		InputStream input = socket.getInputStream() ;
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8")) ;
		String str = null ;
		while((str = reader.readLine()) != null){
			System.out.println(str);
		}
	}
}
