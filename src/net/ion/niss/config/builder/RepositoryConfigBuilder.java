package net.ion.niss.config.builder;

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;

import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.CrakenConfig;
import net.bleujin.rcraken.store.MapConfig;
import net.bleujin.rcraken.store.RedisConfig;
import net.bleujin.rcraken.store.rdb.PGConfig;
import net.bleujin.rcraken.store.rdb.PGCraken;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.config.RepositoryConfig;

public class RepositoryConfigBuilder {

	private String wsName = "admin" ;
	
	private String adminHomeDir = "./resource/admin/" ;
	private String indexHomeDir = "./resource/index/" ;
	private ConfigBuilder parent;

	private Craken craken ;
	
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
			Node fsNode = (Node) xpath.evaluate("store-fs", rconfig, XPathConstants.NODE);
			String lobdir = fsNode.getAttributes().getNamedItem("lobdir").getTextContent() ;
			craken = MapConfig.file(mapDBFile()).lobRootDir(new File(lobdir)).build();
		} else if("redis".equals(store)){
			Node redisNode = (Node) xpath.evaluate("store-redis", rconfig, XPathConstants.NODE);
			// <store-redis address="redis://127.0.0.1:6379" cluster="no" />
			String cluster = redisNode.getAttributes().getNamedItem("cluster").getTextContent() ;
			String address = redisNode.getAttributes().getNamedItem("address").getTextContent() ;
			if ("yes".equals(cluster)) {
				craken = RedisConfig.redisCluster(StringUtil.split(address, ",: ")).build() ;
			} else {
				craken = RedisConfig.redisSingle(address).build() ;
			}
		} else if ("pg".equals(store)) {
			Node pgNode = (Node) xpath.evaluate("store-pg", rconfig, XPathConstants.NODE);
			String jdbcurl = pgNode.getAttributes().getNamedItem("jdbcurl").getTextContent() ;
			String userid = pgNode.getAttributes().getNamedItem("userid").getTextContent() ;
			String userpwd = pgNode.getAttributes().getNamedItem("userpwd").getTextContent() ;
			String lobdir = pgNode.getAttributes().getNamedItem("lobdir").getTextContent() ;
			String cached = pgNode.getAttributes().getNamedItem("cached").getTextContent() ;
			int cacheSize = NumberUtil.toInt(cached, 0) ;
			craken = new PGConfig().jdbcURL(jdbcurl).userId(userid).userPwd(userpwd).lobRootDir(new File(lobdir)).build() ;
			if (cacheSize >= 1000) craken = ((PGCraken)craken).cached(cacheSize) ; // min 1000  
		} else {
			craken = CrakenConfig.mapMemory().build() ;
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
		return new RepositoryConfig(craken, adminHomeDir, indexHomeDir, wsName);
	}
	
	
}
