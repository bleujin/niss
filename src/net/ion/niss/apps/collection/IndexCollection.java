package net.ion.niss.apps.collection;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.common.FieldIndexingStrategy.FieldType;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.reader.InfoReader.InfoHandler;
import net.ion.nsearcher.search.Searcher;
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
				pathBy(colId, wsession)
						.property("indexanalyzer", MyKoreanAnalyzer.class.getCanonicalName())
						.property("queryanalyzer", MyKoreanAnalyzer.class.getCanonicalName()) ;
				return wsession.readSession().pathBy("/webapp/collections/" + colId.idString()) ;
			}

		});
		
		return new IndexCollection(app, colId, central, colNode);
	}

	private static WriteNode pathBy(final ColId colId, WriteSession wsession) {
		return wsession.pathBy("/webapp/collections/" + colId.idString());
	}

	private WriteNode pathBy(WriteSession wsession) {
		return wsession.pathBy(colNode.fqn());
	}
	
	
	

	public Analyzer indexAnalyzer() {
		return central.indexConfig().indexAnalyzer() ;
	}

	public Analyzer queryAnalyzer() {
		return central.searchConfig().queryAnalyzer() ;
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
	

	public IndexCollection queryAnalyzer(final Analyzer analyzer) throws Exception {
		colNode = colNode.session().tranSync(new TransactionJob<ReadNode>() {
			@Override
			public ReadNode handle(WriteSession wsession) throws Exception {
				pathBy(wsession).property("queryanalyzer", analyzer.getClass().getCanonicalName()) ;
				return wsession.readSession().pathBy(colNode.fqn());
			}

		}) ;
		
		central.searchConfig().queryAnalyzer(analyzer) ;
		return this ;
	}
	

	public ReadNode infoNode() {
		return colNode;
	}
	
	public IndexCollection index(JsonObject json) {
		return index(FieldSchema.DEFAULT, json) ;
	}

	public IndexCollection index(FieldSchema fieldSchema, JsonObject son) {
		return index(fieldSchema, new JsonArray().add(son)) ;
	}

	public IndexCollection index(final FieldSchema fieldSchema, final JsonArray jarray) {
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				for(JsonElement jelement : jarray.toArray()){
					JsonObject json = jelement.getAsJsonObject() ;
					
					WriteDocument wdoc = json.has("id") ? isession.newDocument(json.asString("id")) : isession.newDocument() ; 
					for (String key : json.keySet()) {
//						if ("id".equals(key)) continue ;
						JsonElement value = json.get(key);
						if (value.isJsonArray()){
							for(JsonElement jele : value.getAsJsonArray().toArray()){
								wdoc.add(fieldSchema.toMyField(key, jele.getAsJsonPrimitive())) ;
							}
						} else if (value.isJsonPrimitive()){
							wdoc.add(fieldSchema.toMyField(key, value.getAsJsonPrimitive())) ;
						} else { //
							continue ;
						}
					}
					
					isession.updateDocument(wdoc) ;
				}
				return null;
			}
		}) ;

		return this ;
	}
	
	public IndexCollection index(final FieldSchema fieldSchema, final JsonReader jreader) {
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				jreader.beginArray(); 
				while(jreader.hasNext()){
					JsonObject json = jreader.nextJsonObject() ;
					
					WriteDocument wdoc = json.has("id") ? isession.newDocument(json.asString("id")) : isession.newDocument() ; 
					for (String key : json.keySet()) {
//						if ("id".equals(key)) continue ;
						JsonElement value = json.get(key);
						if (value.isJsonArray()){
							for(JsonElement jele : value.getAsJsonArray().toArray()){
								if (! jele.isJsonPrimitive()) continue ;
								wdoc.add(fieldSchema.toMyField(key, jele.getAsJsonPrimitive())) ;
							}
						} else if (value.isJsonPrimitive()){
							wdoc.add(fieldSchema.toMyField(key, value.getAsJsonPrimitive())) ;
						} else { //
							continue ;
						}
					}
					
					isession.updateDocument(wdoc) ;
				}
				jreader.endArray(); 
				return null;
			}
		}) ;		
		return this ;
	}


	public Searcher searcher() throws IOException{
		return central.newSearcher() ;
	}
	

	public <T> T info(InfoHandler<T> infoHandler) throws IOException {
		return central.newReader().info(infoHandler) ;
	}

	public JsonObject status() throws IOException {
		return info(new InfoHandler<JsonObject>() {
			@Override
			public JsonObject view(IndexReader ireader, DirectoryReader dreader) throws IOException {
				JsonObject json = new JsonObject() ;
				
				json.put("Max Doc", dreader.maxDoc()) ;
				json.put("Nums Docs", dreader.numDocs()) ;
				json.put("Deleted Docs",  dreader.numDeletedDocs()) ;
				json.put("Version", dreader.getVersion()) ;
				json.put("Segment Count", dreader.getIndexCommit().getSegmentCount()) ;
				json.put("Current", dreader.isCurrent()) ;
				
				return json;
			}
		});
	}

	public JsonObject dirInfo() throws IOException {
		return info(new InfoHandler<JsonObject>() {
			@Override
			public JsonObject view(IndexReader ireader, DirectoryReader dreader) throws IOException {
				JsonObject json = new JsonObject() ;
				
				json.put("LockFactory", dreader.directory().getLockFactory().getClass().getCanonicalName()) ;
				json.put("Diretory Impl", dreader.directory().getClass().getCanonicalName()) ;
				
				return json;
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

	public String updateExplain(final String field, final String content) throws Exception {
		colNode = colNode.session().tranSync(new TransactionJob<ReadNode>() {
			@Override
			public ReadNode handle(WriteSession wsession) throws Exception {
				pathBy(wsession).property(field, content) ;
				return wsession.readSession().pathBy(colNode.fqn());
			}

		}) ;
		return content ;
	}

	public String propAsString(String field) {
		return colNode.property(field).asString();
	}


	public JsonArray indexAnalyer(){
		JsonArray result = new JsonArray() ;
		List<Class<? extends Analyzer>> list = app.analyzers() ;
		String selected = propAsString("indexanalyzer") ;
		for (Class<? extends Analyzer> clz : list) {
			JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", clz.getCanonicalName().equals(selected)) ;
			result.add(json) ;
		}

		return result ;
	}



}
