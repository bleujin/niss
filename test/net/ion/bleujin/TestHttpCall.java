package net.ion.bleujin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import junit.framework.TestCase;

public class TestHttpCall extends TestCase {

	public void testCall() throws Exception {
		Socket socket = new Socket(InetAddress.getByName("61.250.201.157"), 9000);
		OutputStream output = socket.getOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
		writer.write("GET /img/avatars/sunny.png HTTP/1.0\n");
		writer.write("Host: www.daum.net\n\n");
		writer.flush();

		InputStream input = socket.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
		String str = null;
		while ((str = reader.readLine()) != null) {
			System.out.println(str);
		}
	}

	public void testSocketServer() throws Exception {
		ServerSocket serverSocket = new ServerSocket(10000);
		while (true) {
	        Socket clientSocket = serverSocket.accept();
	        System.err.println("client connected");

	        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

	        String s;
	        while ((s = in.readLine()) != null) {
	            System.out.println(s);
	            if (s.isEmpty()) {
	                break;
	            }
	        }

	        out.write("HTTP/1.0 200 OK\r\n");
	        out.write("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n");
	        out.write("Server: myserver\r\n");
	        out.write("Content-Type: text/html\r\n");
	        out.write("Content-Length: 30\r\n");
//	        out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
//	        out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
	        out.write("\r\n");
	        out.write("<TITLE>Exemple</TITLE>");
	        out.write("<P>Hello Bleujin</P>");

	        out.close();
	        in.close();
	        clientSocket.close();
	    }
	}
}
