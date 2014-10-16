package net.ion.niss.config.builder;

import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.config.ServerConfig;

import org.w3c.dom.Node;

public class ServerConfigBuilder {

	private String id = "niss";
	private int port = 9000 ;
	private ConfigBuilder parent;

	public ServerConfigBuilder(ConfigBuilder parent) {
		this.parent = parent ;
	}
	
	public ServerConfigBuilder node(Node node) {
		String id = node.getAttributes().getNamedItem("id").getTextContent();
		int port = NumberUtil.toInt(node.getAttributes().getNamedItem("port").getTextContent(), 9000) ;
		return id(id).port(port) ;
	}
	
	public ServerConfigBuilder port(int port){
		this.port = port ;
		return this ;
	}
	
	public ServerConfigBuilder id(String id){
		this.id = StringUtil.defaultIfEmpty(id, "niss") ;
		return this ;
	}
	
	public ConfigBuilder parent(){
		return parent ;
	}

	public ServerConfig build() {
		return new ServerConfig(id, port);
	}
}
