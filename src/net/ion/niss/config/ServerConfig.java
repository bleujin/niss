package net.ion.niss.config;


public class ServerConfig {

	private String id = "niss";
	private int port = 9000 ;

	public ServerConfig(String id, int port) {
		this.id = id ;
		this.port = port ;
	}
	
	public int port(){
		return port;
	}
	
	public String id() {
		return id ;
	}


	
}
