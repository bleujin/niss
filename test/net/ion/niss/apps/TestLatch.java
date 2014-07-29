package net.ion.niss.apps;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestLatch extends TestCase {

	
	public void testCountDown() throws Exception {
		
		Client c = new Client() ;
		c.connect(); 
		final CountDownLatch cd = new CountDownLatch(2) ;
		
		new Thread(){
			public void run(){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Debug.line("connected");
				cd.countDown(); 
			}
		}.run();
		
		cd.await(10, TimeUnit.SECONDS); 
		c.sendMessage(); 
	}
}


class Client {
	
	public void connect(){
		
	}
	
	public void sendMessage(){
		Debug.line("send Message");
	}
	
}
