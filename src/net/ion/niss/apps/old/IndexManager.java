package net.ion.niss.apps.old;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.apps.IdString;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;
import net.ion.rosetta.query.Constants;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.util.Version;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import scala.collection.parallel.ParIterableLike.CreateScanTree;

public class IndexManager implements Closeable{

	private final File homeDir = new File("./resource/collection");
	private final File dataDir = new File("./resource/data");
	private Repository r;
	private String wname;
	private Map<IdString, IndexCollection> colMaps = MapUtil.newMap();
	private boolean testMode = false ;
	
	private IndexManager(Repository r, String wname) {
		this.r = r;
		this.wname = wname;
	}

	public Collection<File> listFiles(IdString cid) {
		return FileUtil.listFiles(new File(homeDir, cid.idString()), new String[] { "txt" }, false);
	}

	public File viewFile(IdString cid, String fileName) {
		return new File(new File(homeDir, cid.idString()), fileName);
	}

	public static IndexManager create() throws CorruptIndexException, IOException {

		RepositoryImpl r = createSolo() ;
		r.start();
		final IndexManager created = new IndexManager(r, "admin");

		ReadSession session = r.login("admin");
		session.ghostBy("/webapp/collections/").children().eachNode(new ReadChildrenEach<Void>() {
			@Override
			public Void handle(ReadChildrenIterator iter) {
				while (iter.hasNext()) {
					try {
						ReadNode colNode = iter.next();
						final IdString colId = IdString.create(colNode.fqn().name());
						
						Class<?> indexAnalClz = Class.forName(colNode.property("indexanalyzer").defaultValue(MyKoreanAnalyzer.class.getCanonicalName())) ;
						Analyzer indexAnal = (Analyzer) indexAnalClz.getConstructor(Version.class).newInstance(created.version()) ;

						Class<?> queryAnalClz = Class.forName(colNode.property("queryanalyzer").defaultValue(MyKoreanAnalyzer.class.getCanonicalName())) ;
						Analyzer queryAnal = (Analyzer) queryAnalClz.getConstructor(Version.class).newInstance(created.version()) ;

						Central central = created.createCentral(created.collectionHome(colId), indexAnal, queryAnal) ;
						
						
						created.colMaps.put(colId, IndexCollection.load(created, central, colId, colNode));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				return null;
			}
		});

		return created;
	}
	
	public static IndexManager test() throws IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("admin", ISearcherWorkspaceConfig.create().location(""));
		r.start();
		
		final IndexManager created = new IndexManager(r, "admin");
		created.testMode = true ;
		return created ;
	}

	
	public IndexCollection newCollection(String cid) throws Exception {
		if(hasCollection(cid)) return find(cid) ;
		
		
		ReadSession rsession = r.login(wname);
		final IdString colId = IdString.create(cid);
		
		Central central = createCentral(collectionHome(colId), new MyKoreanAnalyzer(version()), new MyKoreanAnalyzer(version())) ;

		
		ReadNode colNode = rsession.tranSync(new TransactionJob<ReadNode>() {
			@Override
			public ReadNode handle(WriteSession wsession) {
				wsession.pathBy("/webapp/collections/" + colId.idString())
						.property("indexanalyzer", MyKoreanAnalyzer.class.getCanonicalName())
						.property("queryanalyzer", MyKoreanAnalyzer.class.getCanonicalName()) ;
				return wsession.readSession().pathBy("/webapp/collections/" + colId.idString()) ;
			}
		});

		IndexCollection created = IndexCollection.load(this, central, colId, colNode);
		File collectionHome = new File(homeDir, colId.idString());
		if (!collectionHome.exists()) {
			collectionHome.mkdirs();
			FileUtil.copyDirectory(homeDir, collectionHome, FileFilterUtils.suffixFileFilter(".txt"), false);
		}

		colMaps.put(colId, created);
		return created;
	}
	
	private Central createCentral(File collectionHome, Analyzer indexAnalyzer, Analyzer queryAnalyser) throws CorruptIndexException, IOException {
		if (testMode) 
			return CentralConfig.newRam() //newRam()  
				.indexConfigBuilder().indexAnalyzer(indexAnalyzer).parent()
				.searchConfigBuilder().queryAnalyzer(queryAnalyser)
				.build() ; 
		else return CentralConfig.newLocalFile().dirFile(collectionHome) //newRam()  
			.indexConfigBuilder().indexAnalyzer(indexAnalyzer).parent()
			.searchConfigBuilder().queryAnalyzer(queryAnalyser)
			.build() ;
	}

	public void close(){
		for (IndexCollection ic : colMaps.values()) {
			ic.close(); 
		}
		r.shutdown() ;
	}
	
	
	private static RepositoryImpl createDis() throws IOException {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder()
					.transport().clusterName("craken").nodeName("emanon").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		RepositoryImpl r = RepositoryImpl.create(gconfig, "niss");
		r.defineWorkspace("admin", ISearcherWorkspaceConfig.create().location("./resource/admin"));
		return r ;
	}

	private static RepositoryImpl createSolo() throws IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("admin", ISearcherWorkspaceConfig.create().location("./resource/admin"));
		return r;
	}

	public boolean hasCollection(String cid) {
		return colMaps.containsKey(IdString.create(cid));
	}

	public Map<IdString, IndexCollection> cols(){
		return Collections.unmodifiableMap(colMaps) ;
	}
	
	public IndexCollection find(String cid) {
		IndexCollection result = colMaps.get(IdString.create(cid));
		if (result == null)
			throw new IllegalArgumentException("not found collection : " + cid);
		return result;
	}

	public void removeCollection(String cid) throws Exception {
		IndexCollection forRemove = colMaps.remove(IdString.create(cid));
		if (forRemove == null)
			return;
		forRemove.removeSelf();
	}

	void removeCollection(ReadSession session, final Fqn fqn, IdString colId, IndexCollection indexCollection) throws Exception {
		FileUtil.deleteDirectory(new File(homeDir, colId.idString()));
		FileUtil.deleteDirectory(new File(dataDir, colId.idString()));

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession writesession) throws Exception {
				writesession.pathBy(fqn).removeSelf();
				return null;
			}
		});

	}



	public Version version() {
		return SearchConstant.LuceneVersion;
	}

	public void shutdown() {
		for (IndexCollection ic : colMaps.values()) {
			ic.close();
		}
		r.shutdown();
	}

	private static List<Class<? extends Analyzer>> analyzers = ListUtil.<Class<? extends Analyzer>> toList(MyKoreanAnalyzer.class, StandardAnalyzer.class, CJKAnalyzer.class, WhitespaceAnalyzer.class, SimpleAnalyzer.class);

	public List<Class<? extends Analyzer>> analyzers() {
		return analyzers;
	}

	public File collectionHome(IdString colId) {
		return new File(dataDir, colId.idString());
	}

}
