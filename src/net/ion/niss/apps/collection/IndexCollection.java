package net.ion.niss.apps.collection;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.util.IOUtil;
import net.ion.niss.apps.IdString;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.reader.InfoReader.InfoHandler;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.Version;

public class IndexCollection implements Closeable {

	private IndexCollectionApp app;
	private IdString colId;
	private Central central;
	private ReadSession session;
	private final Fqn fqn;
	private IndexCollection(IndexCollectionApp app, IdString colId, Central central, ReadNode colNode) {
		this.app = app ;
		this.colId = colId ;
		this.central = central ;
		this.session = colNode.session() ;
		this.fqn = colNode.fqn() ;
	}

	public static IndexCollection load(IndexCollectionApp app, Central central, IdString colId, ReadNode colNode) throws Exception{
		return new IndexCollection(app, colId, central, colNode) ;
	}

	private WriteNode pathBy(WriteSession wsession) {
		return wsession.pathBy(fqn);
	}
	
	public ReadNode infoNode() {
		return session.pathBy(fqn);
	}

	public IndexCollection indexAnalyzer(final Analyzer analyzer) throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(fqn).property("indexanalyzer", analyzer.getClass().getCanonicalName()) ;
				return null ;
			}
		}) ;
		
		central.indexConfig().indexAnalyzer(analyzer) ;
		return this ;
	}
	

	public IndexCollection queryAnalyzer(final Analyzer analyzer) throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				pathBy(wsession).property("queryanalyzer", analyzer.getClass().getCanonicalName()) ;
				return null;
			}

		}) ;
		
		central.searchConfig().queryAnalyzer(analyzer) ;
		return this ;
	}
	
	public Analyzer indexAnalyzer() {
		return central.indexConfig().indexAnalyzer() ;
	}

	public Analyzer queryAnalyzer() {
		return central.searchConfig().queryAnalyzer() ;
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
		app.removeCollection(session, fqn, colId, this);
	}

	public String updateExplain(final String field, final String content) throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				pathBy(wsession).property(field, content) ;
				return null;
			}

		}) ;
		return content ;
	}

	public String propAsString(String field) {
		return session.pathBy(fqn).property(field).asString();
	}

	public JsonArray analyzerList(){
		JsonArray result = new JsonArray() ;
		List<Class<? extends Analyzer>> list = app.analyzers() ;
		String selected = propAsString("indexanalyzer") ;
		for (Class<? extends Analyzer> clz : list) {
			JsonObject json = new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName()).put("selected", clz.getCanonicalName().equals(selected)) ;
			result.add(json) ;
		}

		return result ;
	}

	public JsonArray termAnalyzer(String content, String clzName, boolean stopword) throws IOException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		Class<? extends Analyzer> aclz = (Class<? extends Analyzer>) Class.forName(clzName) ;
		Analyzer analyzer = aclz.getConstructor(Version.class).newInstance(SearchConstant.LuceneVersion) ;
		
		TokenStream tokenStream = analyzer.tokenStream("text", content);
		OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		JsonArray result = new JsonArray() ;
		while (tokenStream.incrementToken()) {
		    int startOffset = offsetAttribute.startOffset();
		    int endOffset = offsetAttribute.endOffset();
		    result.add(new JsonObject().put("term", charTermAttribute.toString()).put("start", startOffset).put("end", endOffset));
		}
		IOUtil.close(tokenStream);
		IOUtil.close(analyzer);
		return result ;
	}

	public Indexer indexer() {
		return central.newIndexer() ;
	}

	public Central central() {
		return central ;
	}



}
