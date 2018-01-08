package net.ion.niss.config.builder;

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;

import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.store.MapConfig;
import net.bleujin.rcraken.store.RedisConfig;
import net.ion.framework.util.StringUtil;
import net.ion.niss.config.RepositoryConfig;

public class RepositoryConfigBuilder {

	private String wsName = "admin" ;
	
	private String adminHomeDir = "./resource/admin/" ;
	private String indexHomeDir = "./resource/index/" ;
	private ConfigBuilder parent;

	private CrakenConfig crakenConfig ;
	
	public RepositoryConfigBuilder(ConfigBuilder parent){
		this.parent = parent ;
	}
	
	public RepositoryConfigBuilder node(Node rconfig) throws XPathExpressionException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		
		Node adminNode = (Node) xpath.evaluate("admin-home", rconfig, XPathConstants.NODE);
		Node indexNode = (Node) xpath.evaluate("index-home", rconfig, XPathConstants.NODE);
		
		String wname = rconfig.getAttributes().getNamedItem("wsname").getTextContent() ;
		String store = rconfig.getAttributes().getNamedItem("store") == null ?  "memory" : rconfig.getAttributes().getNamedItem("store").getTextContent() ;
		
		adminHomeDir(adminNode.getTextContent()).indexHomeDir(indexNode.getTextContent()).wsName(wname) ;
		

		if ("fs".equals(store)) {
			crakenConfig = MapConfig.file(mapDBFile());
		} else if("redis".equals(store)){
			Node redisNode = (Node) xpath.evaluate("store-redis", rconfig, XPathConstants.NODE);
			// <store-redis address="redis://127.0.0.1:6379" cluster="no" />
			String cluster = redisNode.getAttributes().getNamedItem("cluster").getTextContent() ;
			String address = redisNode.getAttributes().getNamedItem("address").getTextContent() ;
			if ("yes".equals(cluster)) {
				crakenConfig = RedisConfig.redisCluster(StringUtil.split(address, ",: ")) ;
			} else {
				crakenConfig = RedisConfig.redisSingle(address) ;
			}
		} else {
			crakenConfig = CrakenConfig.mapMemory() ;
		}
		
		return this ;
	}

	private File mapDBFile(){
		File adminHome = new File(adminHomeDir);
		if (! adminHome.exists()) {
			adminHome.mkdirs() ;
		}
		return new File(adminHomeDir, "mapdb.db") ;
	}

	
	public RepositoryConfigBuilder adminHomeDir(String adminHomeDir){
		this.adminHomeDir = StringUtil.defaultIfEmpty(adminHomeDir, "./resource/admin/") ;
		return this ;
	}

	public RepositoryConfigBuilder indexHomeDir(String indexHomeDir){
		this.indexHomeDir = StringUtil.defaultIfEmpty(indexHomeDir, "./resource/index/") ;
		return this ;
	}

	public RepositoryConfigBuilder wsName(String wsName){
		this.wsName = StringUtil.defaultIfEmpty(wsName, "admin") ;
		return this ;
	}
	
	
	public ConfigBuilder parent(){
		return parent ;
	}


	public RepositoryConfig build() {
		return new RepositoryConfig(crakenConfig, adminHomeDir, indexHomeDir, wsName);
	}
	
	
}
