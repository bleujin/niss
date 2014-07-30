package net.ion.niss.apps;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.IOUtil;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.reader.InfoReader.InfoHandler;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.util.Version;

public class IndexCollection implements Closeable {

	private CollectionApp app;
	private ColId colId;
	private Central central;
	private ReadNode colNode;
	private IndexCollection(CollectionApp app, ColId colId, Central central, ReadNode colNode) {
		this.app = app ;
		this.colId = colId ;
		this.central = central ;
		this.colNode = colNode ;
	}

	public static IndexCollection load(CollectionApp app, ColId colId, ReadNode colNode) throws Exception{
		Class<?> indexAnalClz = Class.forName(colNode.property("indexanalyzer").defaultValue(MyKoreanAnalyzer.class.getCanonicalName())) ;
		Analyzer indexAnal = (Analyzer) indexAnalClz.getConstructor(Version.class).newInstance(app.version()) ;

		Class<?> queryAnalClz = Class.forName(colNode.property("queryanalyzer").defaultValue(MyKoreanAnalyzer.class.getCanonicalName())) ;
		Analyzer queryAnal = (Analyzer) queryAnalClz.getConstructor(Version.class).newInstance(app.version()) ;

		
		Central central = CentralConfig.newLocalFile().dirFile(app.collectionHome(colId))
				.indexConfigBuilder().indexAnalyzer(indexAnal).parent()
				.searchConfigBuilder().queryAnalyzer(queryAnal)
				.build() ;
		
		return new IndexCollection(app, colId, central, colNode) ;
	}
	
	public static IndexCollection createNew(CollectionApp app, ReadSession rsession, final ColId colId) throws Exception {
		Central central = CentralConfig.newLocalFile().dirFile(app.collectionHome(colId)) //newRam()  
				.indexConfigBuilder().indexAnalyzer(new MyKoreanAnalyzer(app.version())).parent()
				.searchConfigBuilder().queryAnalyzer(new MyKoreanAnalyzer(app.version()))
				.build() ;

		
		ReadNode colNode = rsession.tranSync(new TransactionJob<ReadNode>() {
			@Override
			public ReadNode handle(WriteSession wsession) {
				WriteNode wnode = wsession.pathBy("/webapp/collections/" + colId.idString())
						.property("indexanalyzer", MyKoreanAnalyzer.class.getCanonicalName())
						.property("queryanalyzer", MyKoreanAnalyzer.class.getCanonicalName()) ;
				return wsession.readSession().pathBy("/webapp/collections/" + colId.idString()) ;
			}
		});
		
		return new IndexCollection(app, colId, central, colNode);
	}

	public Analyzer indexAnalyzer() {
		return central.indexConfig().indexAnalyzer() ;
	}

	public IndexCollection indexAnalyzer(final Analyzer analyzer) throws Exception {
		colNode = colNode.session().tranSync(new TransactionJob<ReadNode>() {
			@Override
			public ReadNode handle(WriteSession wsession) throws Exception {
				wsession.pathBy(colNode.fqn()).property("indexanalyzer", analyzer.getClass().getCanonicalName()) ;
				return wsession.readSession().pathBy(colNode.fqn());
			}
		}) ;
		
		central.indexConfig().indexAnalyzer(analyzer) ;
		return this ;
	}
	

	public Analyzer queryAnalyzer() {
		return central.searchConfig().queryAnalyzer() ;
	}

	public IndexCollection queryAnalyzer(final Analyzer analyzer) throws Exception {
		colNode = colNode.session().tranSync(new TransactionJob<ReadNode>() {
			@Override
			public ReadNode handle(WriteSession wsession) throws Exception {
				wsession.pathBy(colNode.fqn()).property("queryanalyzer", analyzer.getClass().getCanonicalName()) ;
				return wsession.readSession().pathBy(colNode.fqn());
			}
		}) ;
		
		central.searchConfig().queryAnalyzer(analyzer) ;
		return this ;

	}
	

	public ReadNode infoNode() {
		return colNode;
	}
	

	public IndexCollection mergeDocument(final String id, final JsonObject values) {
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				
				
				WriteDocument wdoc = isession.newDocument(id).add(values) ;
				isession.updateDocument(wdoc) ;
				return null;
			}
		}) ;
//		colNode.session().tran(new TransactionJob<Void>() {
//			@Override
//			public Void handle(WriteSession wsession) throws Exception {
//				wsession.pathBy(colNode.fqn()).child("nodes").child(id).fromJson(values) ;
//				return null;
//			}
//		}) ;

		return this ;
	}

	public ReadDocument findNode(String id) throws IOException, ParseException {
//		return colNode.child("nodes").child(id) ;
		return central.newSearcher().createRequestByKey(id).findOne() ;
	}

	public <T> T info(InfoHandler<T> infoHandler) throws IOException {
		return central.newReader().info(infoHandler) ;
	}

	public Map<String, Object> status() throws IOException {
		return info(new InfoHandler<Map<String, Object>>() {
			@Override
			public Map<String, Object> view(IndexReader ireader, DirectoryReader dreader) throws IOException {
				Map<String, Object> map = new LinkedHashMap<String, Object>() ;
				
				map.put("Max Doc", dreader.maxDoc()) ;
				map.put("Nums Docs", dreader.numDocs()) ;
				map.put("Deleted Docs",  dreader.numDeletedDocs()) ;
				map.put("Version", dreader.getVersion()) ;
				map.put("Segment Count", dreader.getIndexCommit().getSegmentCount()) ;
				map.put("Current", dreader.isCurrent()) ;
				
				return map;
			}
		});
	}

	public Map<String, Object> dirInfo() throws IOException {
		return info(new InfoHandler<Map<String, Object>>() {
			@Override
			public Map<String, Object> view(IndexReader ireader, DirectoryReader dreader) throws IOException {
				Map<String, Object> map = new LinkedHashMap<String, Object>() ;
				
				map.put("LockFactory", dreader.directory().getLockFactory().getClass().getCanonicalName()) ;
				map.put("Diretory Impl", dreader.directory().getClass().getCanonicalName()) ;
				
				return map;
			}
		});
	}

	public Collection<File> fileList() {
		return app.listFiles(colId) ;
	}

	public void close() {
		IOUtil.close(central);
	}

	public void removeSelf() throws Exception {
		close(); 
		app.removeCollection(colNode, colId, this);
	}



}
