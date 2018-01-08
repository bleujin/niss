package net.ion.niss.config.builder;

import org.w3c.dom.Node;

import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.config.ServerConfig;

public class ServerConfigBuilder {

	private String id = "niss";
	private int port = 9000 ;
	private String password = "dkdldhs" ;
	private ConfigBuilder parent;

	public ServerConfigBuilder(ConfigBuilder parent) {
		this.parent = parent ;
	}
	
	public ServerConfigBuilder node(Node node) {
		String id = node.getAttributes().getNamedItem("id").getTextContent();
		int port = NumberUtil.toInt(node.getAttributes().getNamedItem("port").getTextContent(), 9000) ;
		Node pnode = node.getAttributes().getNamedItem("password");
		return id(id).port(port).password(pnode) ;
	}
	
	private ServerConfigBuilder password(Node pnode) {
		if (pnode != null) this.password = StringUtil.defaultIfEmpty(pnode.getTextContent(), "dkdldhs") ;
		return this;
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
		return new ServerConfig(id, port, password);
	}
}
