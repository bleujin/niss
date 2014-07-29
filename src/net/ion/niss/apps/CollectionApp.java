package net.ion.niss.apps;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.util.Version;

public class CollectionApp {

	private final File homeDir = new File("./resource/collection") ;
	private Repository r;
	private String wname;
	
	private CollectionApp(Repository r, String wname){
		this.r = r ;
		this.wname= wname ;
	} 
	
	public Collection<File> listFiles(ColId cid){
		return FileUtil.listFiles(homeDir, new String[]{"txt"}, false) ;
	}

	public static CollectionApp create() throws CorruptIndexException, IOException {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;
		return new CollectionApp(r, "test");
	}

	public IndexCollection newCollection(String cid) throws Exception {
		
		ReadSession rsession = r.login(wname) ;
		final ColId colId = ColId.create(cid) ;
		
		return IndexCollection.createNew(this, rsession, colId) ;
	}
	
	public Version version(){
		return Version.LUCENE_44 ;
	}

	public void shutdown() {
		r.shutdown() ;
	}

	private List<Class<? extends Analyzer>> analyzers = ListUtil.<Class<? extends Analyzer>>toList(MyKoreanAnalyzer.class, StandardAnalyzer.class, CJKAnalyzer.class, WhitespaceAnalyzer.class) ; 
	public List<Class<? extends Analyzer>> analyzers() {
		return analyzers ;
	}
	
}
