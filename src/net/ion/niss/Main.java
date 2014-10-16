package net.ion.niss;

import java.util.concurrent.ExecutionException;

import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;
import net.ion.radon.Options;

public class Main {

	public static void main(String[] args) throws Exception {
		
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		
		Options options = new Options(args) ;
		NSConfig nsconfig = ConfigBuilder.create(options.getString("config", "./resource/config/nsss-config.xml")).build() ;
//		NSConfig nsconfig = ConfigBuilder.create("./resource/config/nsss-config.xml") ;
		final NissServer server = NissServer.create(nsconfig).start() ;
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				try {
					server.shutdown() ;
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		});
		
		
	}
}
