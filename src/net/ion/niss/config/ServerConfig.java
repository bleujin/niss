package net.ion.niss.config;


public class ServerConfig {

	private String id = "niss";
	private int port = 9000 ;
	private String password;

	public ServerConfig(String id, int port, String password) {
		this.id = id ;
		this.port = port ;
		this.password = password ;
	}
	
	public int port(){
		return port;
	}
	
	public String id() {
		return id ;
	}

	public String password(){
		return password ;
	}

	
}
