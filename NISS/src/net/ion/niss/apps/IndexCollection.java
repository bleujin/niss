package net.ion.niss.apps;

import java.io.IOException;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;

public class IndexCollection {

	private Central central;
	private ReadNode colNode;
	private IndexCollection(Central central, ReadNode colNode) {
		this.central = central ;
		this.colNode = colNode ;
	}

	public static IndexCollection createNew(CollectionApp ca, ReadSession rsession, final ColId colId) throws Exception {
		Central central = CentralConfig.newRam()   // .newLocalFile().dirFile(homeDir + "/" + colId.idString())
				.indexConfigBuilder().indexAnalyzer(new MyKoreanAnalyzer(ca.version())).parent()
				.searchConfigBuilder().queryAnalyzer(new MyKoreanAnalyzer(ca.version()))
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
		
		return new IndexCollection(central, colNode);
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



}
