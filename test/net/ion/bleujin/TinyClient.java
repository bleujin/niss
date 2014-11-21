package net.ion.bleujin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

public class TinyClient {
	
	private InetAddress host;
	private int port;

	public TinyClient(InetAddress host, int port) {
		this.host = host ;
		this.port = port ;
	}

	public final static TinyClient local(int port) throws UnknownHostException{
		return new TinyClient(InetAddress.getLocalHost(), port) ;
	}

	public final static TinyClient local9000() throws UnknownHostException{
		return new TinyClient(InetAddress.getLocalHost(), 9000) ;
	}
	
	public void sayHello(String path) throws IOException, UnknownHostException, UnsupportedEncodingException {
		Socket client = new Socket(host, port) ;
		OutputStream output = client.getOutputStream() ;
		output.write(("GET " + path + " HTTP/1.0\r\n" + "host: www.radon.com\r\n\r\n").getBytes("UTF-8"));
		output.flush(); 
		
		InputStream input = client.getInputStream() ;
		String result = IOUtil.toStringWithClose(input) ;
		
		
		
		Debug.line(result);
		client.close();
	}
}
