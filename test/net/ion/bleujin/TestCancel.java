package net.ion.bleujin;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import junit.framework.TestCase;

public class TestCancel extends TestCase {
	
	public void testRun() throws Exception {
		ExecutorService es = Executors.newFixedThreadPool(2) ;
		Future<Void> task = es.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				boolean infinity = true ;
				while(infinity){
					try {
					Thread.sleep(1000);
					} catch(InterruptedException expect){
						Debug.line(expect);
						return null ;
					}
					System.out.println(".");
				}
				return null;
			}
		}) ;
		
		
		Thread.sleep(3000);
		task.cancel(true) ;
		
		new InfinityThread().startNJoin(); 
	}

}
