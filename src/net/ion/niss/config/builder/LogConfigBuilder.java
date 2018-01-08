package net.ion.niss.config.builder;

import org.w3c.dom.Node;

import net.ion.framework.util.StringUtil;
import net.ion.niss.config.LogConfig;

public class LogConfigBuilder {

	
	private String fileLoc = "./resource/log4j.properties";
	private ConfigBuilder parent;

	public LogConfigBuilder(ConfigBuilder parent){
		this.parent = parent ;
	}
	
	public LogConfigBuilder node(Node node){
		return fileLoc(StringUtil.defaultIfEmpty(node.getTextContent(), "./resource/log4j.properties")) ; 
	}
	
	public LogConfigBuilder fileLoc(String fileLoc){
		this.fileLoc = fileLoc ;
		return this ;
	}
	
	public ConfigBuilder parent(){
		return parent ;
	}

	public LogConfig build(){
		return new LogConfig(fileLoc) ;
	}
}
