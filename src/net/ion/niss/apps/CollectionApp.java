package net.ion.niss.apps;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.rosetta.query.Constants;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.util.Version;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class CollectionApp {

	private final File homeDir = new File("./resource/collection") ;
	private final File dataDir = new File("./resource/data") ;
	private Repository r;
	private String wname;
	private Map<ColId, IndexCollection> colMaps = MapUtil.newMap() ; 
	private CollectionApp(Repository r, String wname){
		this.r = r ;
		this.wname= wname ;
	} 

	public Collection<File> listFiles(ColId cid){
		return FileUtil.listFiles(new File(homeDir, cid.idString()), new String[]{"txt"}, false) ;
	}

	public static CollectionApp create() throws CorruptIndexException, IOException {
		System.setProperty("log4j.configuration", "file:./resource/log4j.properties") ;
		
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
				.defaultClusteredBuilder().transport()
				.clusterName("craken").nodeName("emanon")
				.addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
				.build();
		
//		RepositoryImpl r = RepositoryImpl.create(gconfig, "niss") ;
//		r.defineWorkspace("admin", ISearcherWorkspaceConfig.create().location("./resource/admin")) ;

		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss") ;
		r.defineWorkspaceForTest("admin", ISearcherWorkspaceConfig.create().location("./resource/admin")) ;

		r.start() ;
		final CollectionApp created = new CollectionApp(r, "admin");
		
		
		ReadSession session = r.login("admin") ;
		session.ghostBy("/webapp/collections/").children().eachNode(new ReadChildrenEach<Void>() {
			@Override
			public Void handle(ReadChildrenIterator iter) {
				while(iter.hasNext()){
					try {
						ReadNode colNode = iter.next();
						final ColId colId = ColId.create(colNode.fqn().name()) ;
						created.colMaps.put(colId, IndexCollection.load(created, colId, colNode)) ;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				return null;
			}
		}) ;
		
		
		return created;
	}

	
	public static CollectionApp test() throws CorruptIndexException, IOException {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;
		return new CollectionApp(r, "test");
	}

	
	
	public boolean hasCollection(String cid){
		return colMaps.containsKey(ColId.create(cid)) ;
	}
	
	public IndexCollection find(String cid){
		IndexCollection result = colMaps.get(ColId.create(cid));
		if (result == null) throw new IllegalArgumentException("not found collection : " + cid) ;
		return result ;
	}
	
	public void removeCollection(String cid) throws Exception {
		IndexCollection forRemove = colMaps.remove(ColId.create(cid)) ;
		if (forRemove == null) return ;
		forRemove.removeSelf() ;
	}

	void removeCollection(final ReadNode colNode, ColId colId, IndexCollection indexCollection) throws Exception {
		FileUtil.deleteDirectory(new File(homeDir, colId.idString())) ;
		FileUtil.deleteDirectory(new File(dataDir, colId.idString())) ;
		
		colNode.session().tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession writesession) throws Exception {
				writesession.pathBy(colNode.fqn()).removeSelf() ;
				return null;
			}
		}) ;
		
	}

	
	public IndexCollection newCollection(String cid) throws Exception {
		ReadSession rsession = r.login(wname) ;
		final ColId colId = ColId.create(cid) ;
		
		IndexCollection created = IndexCollection.createNew(this, rsession, colId);
		File collectionHome = new File(homeDir, colId.idString()) ;
		if (! collectionHome.exists()) {
			collectionHome.mkdirs() ;
			FileUtil.copyDirectory(homeDir, collectionHome, FileFilterUtils.suffixFileFilter(".txt"), false);
		}
		
		colMaps.put(colId, created) ;
		return created ;
	}
	
	public Version version(){
		return SearchConstant.LuceneVersion ;
	}

	public void shutdown() {
		for(IndexCollection ic : colMaps.values()){
			ic.close() ;
		}
		r.shutdown() ;
	}

	private static List<Class<? extends Analyzer>> analyzers = ListUtil.<Class<? extends Analyzer>>toList(MyKoreanAnalyzer.class, StandardAnalyzer.class, CJKAnalyzer.class, WhitespaceAnalyzer.class) ; 
	public List<Class<? extends Analyzer>> analyzers() {
		return analyzers ;
	}

	public File collectionHome(ColId colId) {
		return new File(dataDir, colId.idString());
	}


	
}
