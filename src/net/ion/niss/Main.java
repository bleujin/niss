package net.ion.niss;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

import net.ion.framework.util.IOUtil;
import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;
import net.ion.radon.Options;
import net.ion.radon.aclient.ListenableFuture;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Response;

public class Main {

	public static void main(String[] args) throws Exception {

		Options options = new Options(args);
		NSConfig nsconfig = ConfigBuilder.create(options.getString("config", "./resource/config/niss-dist-config.xml")).build();
		
		String action = options.getString("action", "restart") ;
		

		try {
			Socket s = new Socket(InetAddress.getLocalHost(), nsconfig.serverConfig().port());
			s.setSoTimeout(400);
			IOUtil.closeQuietly(s);

			// if connected
			NewClient nc = NewClient.create();
			try {
				ListenableFuture<Response> future = nc.prepareGet("http://localhost:" + nsconfig.serverConfig().port() + "/admin/misc/shutdown?time=10&password=" + nsconfig.serverConfig().password()).execute();
				future.get();
				nc.close();
				Thread.sleep(1000);
			} finally {
				nc.close();
			}

		} catch (ConnectException ex) {
			;
		}
		
		if ("shutdown".equals(action)) {
			return ;
		}
		
		
		final NissServer server = NissServer.create(nsconfig).start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					server.shutdown();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		});

	}
}
